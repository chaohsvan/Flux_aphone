package com.example.flux.app.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DIARY("\u65e5\u8bb0", Icons.Default.Edit),
    CALENDAR("\u65e5\u5386", Icons.Default.DateRange),
    TODO("\u5f85\u529e", Icons.AutoMirrored.Filled.List),
    SETTINGS("\u8bbe\u7f6e", Icons.Default.Settings),
}

object AppRoutes {
    const val MAIN = "main"
    const val TRASH = "trash"

    const val EDITOR_ARG_DIARY_ID = "diaryId"
    const val EDITOR_ARG_ENTRY_DATE = "entryDate"
    const val EDITOR_PATTERN = "editor/{$EDITOR_ARG_DIARY_ID}?date={$EDITOR_ARG_ENTRY_DATE}"

    const val ATTACHMENT_QUERY_ARG = "query"
    const val ATTACHMENT_MANAGER_PATTERN = "attachment_manager?$ATTACHMENT_QUERY_ARG={$ATTACHMENT_QUERY_ARG}"

    const val TODO_ARG_ID = "todoId"
    const val TODO_PATTERN = "todo/{$TODO_ARG_ID}"

    fun editor(diaryId: String): String = "editor/$diaryId"

    fun newEditor(entryDate: String): String = "editor/new?date=$entryDate"

    fun attachmentManager(query: String = ""): String {
        return if (query.isBlank()) {
            "attachment_manager"
        } else {
            "attachment_manager?$ATTACHMENT_QUERY_ARG=${Uri.encode(query)}"
        }
    }

    fun todoDetail(todoId: String): String = "todo/$todoId"
}
