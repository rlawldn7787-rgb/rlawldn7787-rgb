package com.woohaeng.board.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woohaeng.board.data.ApiClient
import com.woohaeng.board.data.LoginRequest
import com.woohaeng.board.data.PendingUpload
import com.woohaeng.board.data.RecordDto
import com.woohaeng.board.data.SessionStore
import com.woohaeng.board.data.UploadQueue
import com.woohaeng.board.util.BoardCompositor
import com.woohaeng.board.util.BoardFields
import com.woohaeng.board.util.BoardLayout
import com.woohaeng.board.util.GallerySaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionStore(app)
    private val queue = UploadQueue(app)

    val token: StateFlow<String?> =
        session.token.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userName: StateFlow<String?> =
        session.userName.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val records = MutableStateFlow<List<RecordDto>>(emptyList())
    val selected = MutableStateFlow<RecordDto?>(null)
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val pendingCount = MutableStateFlow(0)

    init {
        refreshPendingCount()
        viewModelScope.launch {
            token.collect { t ->
                if (!t.isNullOrBlank()) {
                    flushQueue()
                    loadRecords()
                }
            }
        }
    }

    fun login(username: String, password: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                val res = ApiClient.api.login(LoginRequest(username, password))
                session.save(res.token, res.user)
                message.value = null
                onDone(true)
            } catch (e: Exception) {
                message.value = e.message ?: "로그인 실패"
                onDone(false)
            } finally {
                busy.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { session.clear() }
        records.value = emptyList()
    }

    fun loadRecords(
        from: String? = null,
        to: String? = null,
        workName: String? = null
    ) {
        val t = token.value ?: return
        viewModelScope.launch {
            busy.value = true
            try {
                val params = mutableMapOf<String, String>()
                from?.takeIf { it.isNotBlank() }?.let { params["from"] = it }
                to?.takeIf { it.isNotBlank() }?.let { params["to"] = it }
                workName?.takeIf { it.isNotBlank() }?.let { params["workName"] = it }
                records.value = ApiClient.api.records("Bearer $t", params).records
                message.value = null
            } catch (e: Exception) {
                message.value = e.message ?: "목록 조회 실패"
            } finally {
                busy.value = false
            }
        }
    }

    fun loadDetail(id: Int) {
        val t = token.value ?: return
        viewModelScope.launch {
            try {
                selected.value = ApiClient.api.record("Bearer $t", id).record
            } catch (e: Exception) {
                message.value = e.message ?: "상세 조회 실패"
            }
        }
    }

    fun uploadComposed(
        sourceBitmap: Bitmap,
        workName: String,
        workType: String,
        location: String,
        content: String,
        workDate: String = LocalDate.now().toString(),
        layout: BoardLayout = BoardLayout(),
        saveToGallery: Boolean = false,
        onDone: (Boolean) -> Unit
    ) {
        val composed = BoardCompositor.compose(
            sourceBitmap,
            BoardFields(workName, workType, location, content, workDate),
            layout
        )

        if (saveToGallery) {
            val app = getApplication<Application>()
            GallerySaver.saveJpeg(app, sourceBitmap, "원본")
            GallerySaver.saveJpeg(app, composed, "보드판")
        }

        val file = File(getApplication<Application>().cacheDir, "upload_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            composed.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        viewModelScope.launch {
            busy.value = true
            val ok = tryUpload(file, workName, workType, location, content, workDate)
            if (!ok) {
                queue.enqueue(
                    PendingUpload(
                        id = UUID.randomUUID().toString(),
                        imagePath = file.absolutePath,
                        workName = workName,
                        workType = workType,
                        location = location,
                        content = content,
                        workDate = workDate
                    )
                )
                refreshPendingCount()
                message.value = if (saveToGallery) {
                    "갤러리 저장됨 · 네트워크 오류로 대기열에 저장했습니다."
                } else {
                    "네트워크 오류로 대기열에 저장했습니다."
                }
            } else {
                message.value = if (saveToGallery) {
                    "업로드 완료 · 원본/보드판 사진을 갤러리에 저장했습니다."
                } else {
                    "업로드 완료"
                }
                loadRecords()
            }
            busy.value = false
            onDone(ok)
        }
    }

    private suspend fun tryUpload(
        file: File,
        workName: String,
        workType: String,
        location: String,
        content: String,
        workDate: String
    ): Boolean {
        val t = token.value ?: return false
        return try {
            val body = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", file.name, body)
            fun text(v: String) = v.toRequestBody("text/plain".toMediaType())
            ApiClient.api.createRecord(
                auth = "Bearer $t",
                workName = text(workName),
                workType = text(workType),
                location = text(location),
                content = text(content),
                workDate = text(workDate),
                image = part
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun flushQueue() {
        viewModelScope.launch {
            val t = token.value ?: return@launch
            if (t.isBlank()) return@launch
            queue.list().forEach { item ->
                val file = File(item.imagePath)
                if (!file.exists()) {
                    queue.remove(item.id)
                    return@forEach
                }
                val ok = tryUpload(
                    file,
                    item.workName,
                    item.workType,
                    item.location,
                    item.content,
                    item.workDate
                )
                if (ok) queue.remove(item.id)
            }
            refreshPendingCount()
            loadRecords()
        }
    }

    fun exportExcel(from: String?, to: String?, workName: String?) {
        val t = token.value ?: return
        viewModelScope.launch {
            busy.value = true
            try {
                val params = mutableMapOf<String, String>()
                from?.takeIf { it.isNotBlank() }?.let { params["from"] = it }
                to?.takeIf { it.isNotBlank() }?.let { params["to"] = it }
                workName?.takeIf { it.isNotBlank() }?.let { params["workName"] = it }
                val body = ApiClient.api.exportExcel("Bearer $t", params)
                val out = File(
                    getApplication<Application>().cacheDir,
                    "woohaeng_records.xlsx"
                )
                out.outputStream().use { body.byteStream().copyTo(it) }
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    out
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(
                    Intent.createChooser(intent, "엑셀 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                message.value = e.message ?: "엑셀 다운로드 실패"
            } finally {
                busy.value = false
            }
        }
    }

    fun decodeUri(uri: Uri): Bitmap? {
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun refreshPendingCount() {
        pendingCount.value = queue.list().size
    }
}
