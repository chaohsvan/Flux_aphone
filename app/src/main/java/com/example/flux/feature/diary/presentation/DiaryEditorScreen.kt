package com.example.flux.feature.diary.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.flux.feature.diary.presentation.component.DiaryMetadataEditor
import com.example.flux.feature.diary.presentation.component.EditorStatsRow
import com.example.flux.feature.diary.presentation.component.MarkdownEditorToolbar
import com.example.flux.feature.diary.presentation.component.MarkdownText
import com.example.flux.feature.diary.presentation.component.applyEditorAction
import com.example.flux.feature.diary.presentation.component.countOccurrences

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

    val lineCount = editorValue.text.lines().size.coerceAtLeast(1)
    val paragraphCount = editorValue.text
        .split(Regex("\\n\\s*\\n"))
        .count { it.isNotBlank() }
        .coerceAtLeast(1)
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

            uiState.errorMessage?.let { errorMessage ->
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
                onAction = { action ->
                    editorValue = applyEditorAction(editorValue, action)
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
