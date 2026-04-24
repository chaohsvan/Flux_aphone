package com.example.flux.feature.diary.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    onNavigateUp: () -> Unit,
    viewModel: DiaryEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPreviewMode by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }
    var highlightQuery by remember { mutableStateOf("") }
    var editorValue by remember(uiState.id, uiState.entryDate) {
        mutableStateOf(TextFieldValue(uiState.contentMd))
    }
    val context = LocalContext.current
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.attachFile(context, uri)
            }
        }
    )

    LaunchedEffect(uiState.contentMd) {
        if (uiState.contentMd != editorValue.text) {
            editorValue = TextFieldValue(
                text = uiState.contentMd,
                selection = TextRange(uiState.contentMd.length)
            )
        }
    }

    val lineCount = max(1, editorValue.text.lines().size)
    val paragraphCount = editorValue.text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }.coerceAtLeast(1)
    val charCount = editorValue.text.filterNot { it.isWhitespace() }.length
    val highlightCount = countOccurrences(editorValue.text, highlightQuery)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.entryDate.ifBlank { "日记" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.id != null) {
                        IconButton(onClick = {
                            viewModel.deleteDiary()
                            onNavigateUp()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Check,
                            contentDescription = if (isPreviewMode) "编辑" else "预览"
                        )
                    }
                    IconButton(onClick = { attachmentLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加附件")
                    }
                    IconButton(onClick = {
                        viewModel.saveDiary(onSaved = onNavigateUp)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                placeholder = { Text("标题") },
                textStyle = MaterialTheme.typography.titleLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            EditorStatsRow(
                isPreviewMode = isPreviewMode,
                isFocusMode = isFocusMode,
                charCount = charCount,
                lineCount = lineCount,
                paragraphCount = paragraphCount,
                highlightCount = highlightCount,
                onToggleFocus = { isFocusMode = !isFocusMode }
            )

            if (!isFocusMode) {
                DiaryMetadataEditor(
                    uiState = uiState,
                    onDateChange = viewModel::updateEntryDate,
                    onTimeChange = viewModel::updateEntryTime,
                    onMoodChange = viewModel::updateMood,
                    onWeatherChange = viewModel::updateWeather,
                    onLocationChange = viewModel::updateLocation,
                    onTagTextChange = viewModel::updateTagText,
                    onFavoriteToggle = viewModel::toggleFavorite
                )
            }

            OutlinedTextField(
                value = highlightQuery,
                onValueChange = { highlightQuery = it },
                label = { Text("关键词定位") },
                placeholder = { Text("输入后在预览中高亮") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            MarkdownEditorToolbar(
                onAction = { transform ->
                    editorValue = transform(editorValue)
                    viewModel.updateContent(editorValue.text)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isPreviewMode) {
                MarkdownText(
                    text = editorValue.text,
                    highlightQuery = highlightQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                )
            } else {
                TextField(
                    value = editorValue,
                    onValueChange = { value ->
                        editorValue = value
                        viewModel.updateContent(value.text)
                    },
                    placeholder = { Text("开始记录你的想法") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EditorStatsRow(
    isPreviewMode: Boolean,
    isFocusMode: Boolean,
    charCount: Int,
    lineCount: Int,
    paragraphCount: Int,
    highlightCount: Int,
    onToggleFocus: () -> Unit
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
            label = { Text(if (isPreviewMode) "预览模式" else "编辑模式") }
        )
        FilterChip(
            selected = isFocusMode,
            onClick = onToggleFocus,
            label = { Text(if (isFocusMode) "专注模式已开" else "专注模式") }
        )
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("字数 $charCount") })
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("行数 $lineCount") })
        FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("段落 $paragraphCount") })
        if (highlightCount > 0) {
            FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("命中 $highlightCount") })
        }
    }
}

@Composable
private fun MarkdownEditorToolbar(
    onAction: ((TextFieldValue) -> TextFieldValue) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarChip("H") { onAction { applyLinePrefix(it, "# ") } }
        ToolbarChip("B") { onAction { wrapSelection(it, "**", "**", "加粗文本") } }
        ToolbarChip("I") { onAction { wrapSelection(it, "*", "*", "斜体文本") } }
        ToolbarChip(">") { onAction { applyLinePrefix(it, "> ") } }
        ToolbarChip("-") { onAction { applyLinePrefix(it, "- ") } }
        ToolbarChip("[ ]") { onAction { applyLinePrefix(it, "- [ ] ") } }
        ToolbarChip("</>") { onAction(::applyCodeStyle) }
        ToolbarChip("Link") { onAction { wrapSelection(it, "[", "](https://)", "链接文本") } }
        ToolbarChip("---") { onAction { insertBlock(it, "\n---\n") } }
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
private fun DiaryMetadataEditor(
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
            OutlinedTextField(
                value = uiState.entryDate,
                onValueChange = onDateChange,
                label = { Text("日期") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.entryTime ?: "",
                onValueChange = onTimeChange,
                label = { Text("时间") },
                placeholder = { Text("HH:mm") },
                singleLine = true,
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
            val moods = listOf("开心", "平静", "低落", "紧张")
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

private fun wrapSelection(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val selected = value.text.substring(start, end)
    val body = selected.ifBlank { placeholder }
    val replacement = prefix + body + suffix
    val updated = value.text.replaceRange(start, end, replacement)
    val bodyStart = start + prefix.length
    return value.copy(
        text = updated,
        selection = TextRange(bodyStart, bodyStart + body.length)
    )
}

private fun applyLinePrefix(
    value: TextFieldValue,
    prefix: String
): TextFieldValue {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val lineStart = value.text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
    val lineEnd = value.text.indexOf('\n', end).let { if (it < 0) value.text.length else it }
    val target = value.text.substring(lineStart, lineEnd)
    val replaced = target.lines().joinToString("\n") { line ->
        if (line.isBlank()) prefix.trimEnd() else prefix + line
    }
    val updated = value.text.replaceRange(lineStart, lineEnd, replaced)
    return value.copy(
        text = updated,
        selection = TextRange(lineStart, lineStart + replaced.length)
    )
}

private fun applyCodeStyle(value: TextFieldValue): TextFieldValue {
    val selected = value.text.substring(
        min(value.selection.start, value.selection.end),
        max(value.selection.start, value.selection.end)
    )
    return if (selected.contains('\n') || selected.isBlank()) {
        wrapSelection(value, "```\n", "\n```", "code")
    } else {
        wrapSelection(value, "`", "`", "code")
    }
}

private fun insertBlock(
    value: TextFieldValue,
    block: String
): TextFieldValue {
    val cursor = value.selection.end
    val updated = value.text.replaceRange(cursor, cursor, block)
    val newCursor = cursor + block.length
    return value.copy(text = updated, selection = TextRange(newCursor))
}

private fun countOccurrences(text: String, query: String): Int {
    val needle = query.trim()
    if (needle.isBlank()) return 0
    var count = 0
    var start = 0
    val source = text.lowercase()
    val target = needle.lowercase()
    while (true) {
        val index = source.indexOf(target, start)
        if (index < 0) return count
        count += 1
        start = index + target.length
    }
}
