package com.woohaeng.board.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.boardLabelStore by preferencesDataStore("board_labels")

data class BoardLabels(
    val workName: String = "공사명",
    val workType: String = "공종",
    val location: String = "위치",
    val content: String = "내용",
    val workDate: String = "일자"
) {
    fun asList(): List<Pair<String, String>> = listOf(
        "workName" to workName,
        "workType" to workType,
        "location" to location,
        "content" to content,
        "workDate" to workDate
    )
}

class BoardLabelStore(private val context: Context) {
    private val workNameKey = stringPreferencesKey("label_work_name")
    private val workTypeKey = stringPreferencesKey("label_work_type")
    private val locationKey = stringPreferencesKey("label_location")
    private val contentKey = stringPreferencesKey("label_content")
    private val workDateKey = stringPreferencesKey("label_work_date")

    val labels: Flow<BoardLabels> = context.boardLabelStore.data.map { prefs ->
        BoardLabels(
            workName = prefs[workNameKey] ?: "공사명",
            workType = prefs[workTypeKey] ?: "공종",
            location = prefs[locationKey] ?: "위치",
            content = prefs[contentKey] ?: "내용",
            workDate = prefs[workDateKey] ?: "일자"
        )
    }

    suspend fun save(labels: BoardLabels) {
        context.boardLabelStore.edit {
            it[workNameKey] = labels.workName.trim().ifBlank { "공사명" }
            it[workTypeKey] = labels.workType.trim().ifBlank { "공종" }
            it[locationKey] = labels.location.trim().ifBlank { "위치" }
            it[contentKey] = labels.content.trim().ifBlank { "내용" }
            it[workDateKey] = labels.workDate.trim().ifBlank { "일자" }
        }
    }
}
