package com.example.flux.feature.diary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    onNavigateUp: () -> Unit,
    viewModel: DiaryEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPreviewMode by remember { mutableStateOf(false) }

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
                    IconButton(onClick = {
                        viewModel.saveDiary()
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    placeholder = { Text("标题...", style = MaterialTheme.typography.titleLarge) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

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

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                if (isPreviewMode) {
                    MarkdownText(
                        text = uiState.contentMd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    TextField(
                        value = uiState.contentMd,
                        onValueChange = viewModel::updateContent,
                        placeholder = { Text("开始记录你的想法...") },
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val moods = listOf("开心", "平静", "伤心", "愤怒")
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
