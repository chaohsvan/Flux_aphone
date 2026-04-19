package com.example.flux.feature.todo.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: TodoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newSubtaskTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveTodo()
                            onNavigateUp()
                        },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                    IconButton(onClick = {
                        viewModel.deleteTodo()
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                TodoEditableFields(
                    uiState = uiState,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onPriorityChange = viewModel::setPriority,
                    onStatusChange = viewModel::setStatus,
                    onStartAtChange = viewModel::updateStartAt,
                    onDueAtChange = viewModel::updateDueAt,
                    onReminderChange = viewModel::updateReminderMinutes
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("子任务", style = MaterialTheme.typography.titleMedium)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.subtasks, key = { it.id }) { subtask ->
                        val isCompleted = subtask.isCompleted == 1
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { viewModel.toggleSubtaskStatus(subtask.id, isCompleted) }
                            )
                            Text(
                                text = subtask.title,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("添加子任务...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(newSubtaskTitle.trim())
                                newSubtaskTitle = ""
                            }
                        },
                        enabled = newSubtaskTitle.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoEditableFields(
    uiState: TodoDetailUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onStartAtChange: (String) -> Unit,
    onDueAtChange: (String) -> Unit,
    onReminderChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            label = { Text("任务") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("备注") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.startAt,
                onValueChange = onStartAtChange,
                label = { Text("开始") },
                placeholder = { Text("YYYY-MM-DD HH:mm") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.dueAt,
                onValueChange = onDueAtChange,
                label = { Text("截止") },
                placeholder = { Text("YYYY-MM-DD HH:mm") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = uiState.reminderMinutesText,
            onValueChange = onReminderChange,
            label = { Text("提醒提前分钟") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = uiState.priority == "normal",
                onClick = { onPriorityChange("normal") },
                label = { Text("普通") }
            )
            FilterChip(
                selected = uiState.priority == "high",
                onClick = { onPriorityChange("high") },
                label = { Text("高优先级") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = uiState.status == "pending",
                onClick = { onStatusChange("pending") },
                label = { Text("待办") }
            )
            FilterChip(
                selected = uiState.status == "in_progress",
                onClick = { onStatusChange("in_progress") },
                label = { Text("进行中") }
            )
            FilterChip(
                selected = uiState.status == "completed",
                onClick = { onStatusChange("completed") },
                label = { Text("完成") }
            )
        }
    }
}
