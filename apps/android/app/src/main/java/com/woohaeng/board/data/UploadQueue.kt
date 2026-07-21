package com.woohaeng.board.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PendingUpload(
    val id: String,
    val imagePath: String,
    val workName: String,
    val workType: String,
    val location: String,
    val content: String,
    val workDate: String
)

/** 네트워크 실패 시 로컬에 담아 두었다가 재전송하는 간단 큐 */
class UploadQueue(private val context: Context) {
    private val file: File
        get() = File(context.filesDir, "pending_uploads.json")

    fun list(): List<PendingUpload> {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PendingUpload(
                id = o.getString("id"),
                imagePath = o.getString("imagePath"),
                workName = o.getString("workName"),
                workType = o.optString("workType"),
                location = o.optString("location"),
                content = o.optString("content"),
                workDate = o.getString("workDate")
            )
        }
    }

    fun enqueue(item: PendingUpload) {
        val current = list().toMutableList()
        current.add(item)
        save(current)
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(items: List<PendingUpload>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("imagePath", item.imagePath)
                    .put("workName", item.workName)
                    .put("workType", item.workType)
                    .put("location", item.location)
                    .put("content", item.content)
                    .put("workDate", item.workDate)
            )
        }
        file.writeText(arr.toString())
    }
}
