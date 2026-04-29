package com.example.flux.feature.diary.presentation.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.example.flux.core.ui.DateField
import com.example.flux.core.ui.TimeField
import com.example.flux.feature.diary.presentation.DiaryEditorUiState

enum class MarkdownEditorAction {
    HEADING,
    BOLD,
    ITALIC,
    QUOTE,
    BULLET,
    TASK,
    CODE,
    LINK,
    DIVIDER
}

@Composable
fun EditorStatsRow(
    isPreviewMode: Boolean,
    isMetadataExpanded: Boolean,
    showMetadataToggle: Boolean,
    onToggleMetadata: () -> Unit,
    charCount: Int,
    lineCount: Int,
    paragraphCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = isPreviewMode,
            onClick = {},
            enabled = false,
            label = { Text(if (isPreviewMode) "专注预览" else "编辑模式") }
        )
        if (showMetadataToggle) {
            FilterChip(
                selected = isMetadataExpanded,
                onClick = onToggleMetadata,
                label = { Text(if (isMetadataExpanded) "\u6536\u8d77\u4fe1\u606f" else "\u65e5\u8bb0\u4fe1\u606f") }
            )
        }
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("字数 $charCount") })
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("行数 $lineCount") })
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("段落 $paragraphCount") })
    }
}

@Composable
fun MarkdownEditorToolbar(
    onAction: (MarkdownEditorAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarChip("H") { onAction(MarkdownEditorAction.HEADING) }
        ToolbarChip("B") { onAction(MarkdownEditorAction.BOLD) }
        ToolbarChip("I") { onAction(MarkdownEditorAction.ITALIC) }
        ToolbarChip(">") { onAction(MarkdownEditorAction.QUOTE) }
        ToolbarChip("-") { onAction(MarkdownEditorAction.BULLET) }
        ToolbarChip("[ ]") { onAction(MarkdownEditorAction.TASK) }
        ToolbarChip("</>") { onAction(MarkdownEditorAction.CODE) }
        ToolbarChip("Link") { onAction(MarkdownEditorAction.LINK) }
        ToolbarChip("---") { onAction(MarkdownEditorAction.DIVIDER) }
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun DiaryMetadataEditor(
    uiState: DiaryEditorUiState,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onMoodChange: (String?) -> Unit,
    onWeatherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onTagTextChange: (String) -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                value = uiState.entryDate,
                onValueChange = onDateChange,
                label = "日期",
                modifier = Modifier.weight(1f)
            )
            TimeField(
                value = uiState.entryTime ?: "",
                onValueChange = onTimeChange,
                label = "时间",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.weather ?: "",
                onValueChange = onWeatherChange,
                label = { Text("天气") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.locationName ?: "",
                onValueChange = onLocationChange,
                label = { Text("位置") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.tagText,
            onValueChange = onTagTextChange,
            label = { Text("标签") },
            placeholder = { Text("用逗号分隔") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val moods = DiaryMoodOptions
            moods.forEach { mood ->
                FilterChip(
                    selected = uiState.mood == mood,
                    onClick = { onMoodChange(if (uiState.mood == mood) null else mood) },
                    label = { Text(mood) }
                )
            }
            FilterChip(
                selected = uiState.isFavorite,
                onClick = onFavoriteToggle,
                label = { Text("收藏") }
            )
        }
    }
}

fun applyEditorAction(
    value: TextFieldValue,
    action: MarkdownEditorAction
): TextFieldValue {
    return when (action) {
        MarkdownEditorAction.HEADING -> applyLinePrefix(value, "# ")
        MarkdownEditorAction.BOLD -> wrapSelection(value, "**", "**", "加粗文本")
        MarkdownEditorAction.ITALIC -> wrapSelection(value, "*", "*", "斜体文本")
        MarkdownEditorAction.QUOTE -> applyLinePrefix(value, "> ")
        MarkdownEditorAction.BULLET -> applyLinePrefix(value, "- ")
        MarkdownEditorAction.TASK -> applyLinePrefix(value, "- [ ] ")
        MarkdownEditorAction.CODE -> applyCodeStyle(value)
        MarkdownEditorAction.LINK -> wrapSelection(value, "[", "](https://)", "链接文本")
        MarkdownEditorAction.DIVIDER -> insertBlock(value, "\n---\n")
    }
}
