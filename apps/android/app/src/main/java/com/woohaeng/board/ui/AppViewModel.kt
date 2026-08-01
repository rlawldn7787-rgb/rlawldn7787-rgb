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
import com.woohaeng.board.data.BoardLabelStore
import com.woohaeng.board.data.BoardLabels
import com.woohaeng.board.data.LoginRequest
import com.woohaeng.board.data.PendingUpload
import com.woohaeng.board.data.RecordDto
import com.woohaeng.board.data.SessionStore
import com.woohaeng.board.data.UploadQueue
import com.woohaeng.board.util.BoardCompositor
import com.woohaeng.board.util.BoardFields
import com.woohaeng.board.util.BoardLayout
import com.woohaeng.board.util.GallerySaver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.max

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionStore(app)
    private val queue = UploadQueue(app)
    private val boardLabelStore = BoardLabelStore(app)
    private val uploadMutex = Mutex()

    val token: StateFlow<String?> =
        session.token.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userName: StateFlow<String?> =
        session.userName.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val boardLabels: StateFlow<BoardLabels> =
        boardLabelStore.labels.stateIn(viewModelScope, SharingStarted.Eagerly, BoardLabels())

    val records = MutableStateFlow<List<RecordDto>>(emptyList())
    val selected = MutableStateFlow<RecordDto?>(null)
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val pendingCount = MutableStateFlow(0)
    val needsRelogin = MutableStateFlow(false)

    fun saveBoardLabels(labels: BoardLabels) {
        viewModelScope.launch { boardLabelStore.save(labels) }
    }

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
                needsRelogin.value = false
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
        needsRelogin.value = false
    }

    private suspend fun handleUnauthorized(detail: String): UploadResult {
        session.clear()
        needsRelogin.value = true
        return UploadResult(
            false,
            "로그인이 만료되었습니다. 다시 로그인해 주세요. ($detail)"
        )
    }

    fun loadRecords(
        from: String? = null,
        to: String? = null,
        workName: String? = null
    ) {
        val t = token.value ?: return
        val ym = YearMonth.now()
        val fromDate = from?.takeIf { it.isNotBlank() } ?: ym.atDay(1).toString()
        val toDate = to?.takeIf { it.isNotBlank() } ?: ym.atEndOfMonth().toString()
        viewModelScope.launch {
            busy.value = true
            try {
                val params = mutableMapOf(
                    "from" to fromDate,
                    "to" to toDate
                )
                workName?.takeIf { it.isNotBlank() }?.let { params["workName"] = it }
                records.value = ApiClient.api.records("Bearer $t", params).records
                // 업로드 오류 문구는 유지 (목록 조회 성공으로 지우지 않음)
            } catch (e: Exception) {
                if (message.value.isNullOrBlank()) {
                    message.value = e.message ?: "목록 조회 실패"
                }
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
            layout,
            boardLabels.value
        )

        if (saveToGallery) {
            val app = getApplication<Application>()
            GallerySaver.saveJpeg(app, sourceBitmap, "원본")
            GallerySaver.saveJpeg(app, composed, "보드판")
        }

        val file = prepareUploadFile(composed)

        viewModelScope.launch {
            busy.value = true
            val result = uploadMutex.withLock {
                tryUpload(file, workName, workType, location, content, workDate)
            }
            if (!result.ok) {
                // 로그인 만료면 대기열에 넣지 않고 재로그인 유도
                if (needsRelogin.value) {
                    runCatching { file.delete() }
                    message.value = result.error
                } else {
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
                    val reason = result.error ?: "알 수 없는 오류"
                    message.value = if (saveToGallery) {
                        "갤러리 저장됨 · 업로드 실패로 대기열에 저장: $reason"
                    } else {
                        "업로드 실패로 대기열에 저장: $reason"
                    }
                }
            } else {
                // 성공한 임시 파일은 정리
                runCatching { file.delete() }
                message.value = if (saveToGallery) {
                    "업로드 완료 · 원본/보드판 사진을 갤러리에 저장했습니다."
                } else {
                    "업로드 완료"
                }
                loadRecords()
            }
            busy.value = false
            onDone(result.ok)
        }
    }

    private data class UploadResult(val ok: Boolean, val error: String? = null)

    private fun prepareUploadFile(bitmap: Bitmap): File {
        val maxSide = 1920
        val longest = max(bitmap.width, bitmap.height).toFloat()
        val scaled =
            if (longest > maxSide) {
                val ratio = maxSide / longest
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
        val dir = File(getApplication<Application>().filesDir, "pending_images").apply { mkdirs() }
        val file = File(dir, "upload_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        return file
    }

    private fun isRetryableNetworkError(message: String?): Boolean {
        val m = message.orEmpty().lowercase()
        return m.contains("stream was reset") ||
            m.contains("connection reset") ||
            m.contains("timeout") ||
            m.contains("unexpected end of stream") ||
            m.contains("software caused connection abort")
    }

    private suspend fun tryUpload(
        file: File,
        workName: String,
        workType: String,
        location: String,
        content: String,
        workDate: String
    ): UploadResult {
        var last = UploadResult(false, "업로드 실패")
        repeat(3) { attempt ->
            last = tryUploadOnce(file, workName, workType, location, content, workDate)
            if (last.ok) return last
            if (needsRelogin.value) return last
            if (last.error?.contains("서버 401") == true) return last
            if (!isRetryableNetworkError(last.error)) return last
            delay(800L * (attempt + 1))
        }
        return last
    }

    private suspend fun tryUploadOnce(
        file: File,
        workName: String,
        workType: String,
        location: String,
        content: String,
        workDate: String
    ): UploadResult {
        val t = token.value
        if (t.isNullOrBlank()) {
            return handleUnauthorized("토큰 없음")
        }
        if (!file.exists() || file.length() == 0L) {
            return UploadResult(false, "업로드 파일이 비어 있습니다")
        }
        return try {
            val body = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", file.name, body)
            fun text(v: String) = v.toRequestBody("text/plain".toMediaType())
            val response = ApiClient.api.createRecord(
                auth = "Bearer $t",
                workName = text(workName),
                workType = text(workType),
                location = text(location),
                content = text(content),
                workDate = text(workDate),
                image = part
            )
            // JSON 파싱 실패와 무관하게 HTTP 2xx면 업로드 성공으로 처리
            if (response.isSuccessful) {
                response.body()?.close()
                UploadResult(true)
            } else {
                val err = runCatching { response.errorBody()?.string() }.getOrNull()
                if (response.code() == 401) {
                    handleUnauthorized(err ?: "401")
                } else {
                    UploadResult(
                        false,
                        "서버 ${response.code()}${if (!err.isNullOrBlank()) ": $err" else ""}"
                    )
                }
            }
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            if (e.code() == 401) {
                handleUnauthorized(body ?: "401")
            } else {
                UploadResult(
                    false,
                    "서버 ${e.code()}${if (!body.isNullOrBlank()) ": $body" else ""}"
                )
            }
        } catch (e: Exception) {
            UploadResult(
                false,
                e.message ?: e.javaClass.simpleName
            )
        }
    }

    fun flushQueue() {
        viewModelScope.launch {
            val t = token.value ?: return@launch
            if (t.isBlank()) return@launch
            uploadMutex.withLock {
                queue.list().forEach { item ->
                    if (needsRelogin.value) return@withLock
                    val file = File(item.imagePath)
                    if (!file.exists()) {
                        queue.remove(item.id)
                        return@forEach
                    }
                    val result = tryUpload(
                        file,
                        item.workName,
                        item.workType,
                        item.location,
                        item.content,
                        item.workDate
                    )
                    if (result.ok) {
                        queue.remove(item.id)
                        runCatching { file.delete() }
                    } else {
                        message.value = "대기 재전송 실패: ${result.error}"
                        if (needsRelogin.value) return@withLock
                    }
                }
            }
            refreshPendingCount()
            if (!needsRelogin.value) {
                loadRecords()
            }
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
