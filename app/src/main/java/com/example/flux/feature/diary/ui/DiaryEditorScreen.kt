package com.example.flux.feature.diary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                title = { Text(uiState.entryDate) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.id != null) {
                        IconButton(onClick = {
                            viewModel.deleteDiary()
                            onNavigateUp()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Check, // Reusing icons since compose material default doesn't have explicit Preview/Visibility unless extended
                            contentDescription = "Toggle Preview"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.saveDiary()
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
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
    onFavoriteToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
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
