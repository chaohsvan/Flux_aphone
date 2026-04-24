package com.example.flux.feature.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.example.flux.core.database.entity.TodoHistoryEntity

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveTodo(onSaved = onNavigateUp) },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                    IconButton(
                        onClick = {
                            viewModel.deleteTodo()
                            onNavigateUp()
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TodoEditableFields(
                    uiState = uiState,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onProjectChange = viewModel::updateProjectId,
                    onPriorityChange = viewModel::setPriority,
                    onStatusChange = viewModel::setStatus,
                    onStartAtChange = viewModel::updateStartAt,
                    onDueAtChange = viewModel::updateDueAt,
                    onReminderChange = viewModel::updateReminderMinutes,
                    onRecurrenceChange = viewModel::setRecurrence,
                    onRecurrenceIntervalChange = viewModel::updateRecurrenceInterval,
                    onRecurrenceUntilChange = viewModel::updateRecurrenceUntil
                )
            }

            uiState.errorMessage?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                HorizontalDivider()
                TodoHistorySection(history = uiState.history)
                HorizontalDivider()
                Text("子任务", style = MaterialTheme.typography.titleMedium)
            }

            itemsIndexed(uiState.subtasks, key = { _, subtask -> subtask.id }) { index, subtask ->
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
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.moveSubtaskOrder(subtask.id, moveUp = true) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移子任务")
                    }
                    IconButton(
                        onClick = { viewModel.moveSubtaskOrder(subtask.id, moveUp = false) },
                        enabled = index < uiState.subtasks.lastIndex
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移子任务")
                    }
                    IconButton(onClick = { viewModel.deleteSubtask(subtask.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除子任务",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
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
    onProjectChange: (String?) -> Unit,
    onPriorityChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onStartAtChange: (String) -> Unit,
    onDueAtChange: (String) -> Unit,
    onReminderChange: (String) -> Unit,
    onRecurrenceChange: (String) -> Unit,
    onRecurrenceIntervalChange: (String) -> Unit,
    onRecurrenceUntilChange: (String) -> Unit
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

        Text("项目标签", style = MaterialTheme.typography.titleMedium)
        LazyRow {
            item {
                FilterChip(
                    selected = uiState.projectId == null,
                    onClick = { onProjectChange(null) },
                    label = { Text("无标签") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            items(uiState.projects, key = { it.id }) { project ->
                FilterChip(
                    selected = uiState.projectId == project.id,
                    onClick = { onProjectChange(project.id) },
                    label = { Text(project.name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

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
            label = { Text("提前提醒分钟") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("重复规则", style = MaterialTheme.typography.titleMedium)
        LazyRow {
            items(listOf("none", "daily", "weekly", "monthly", "yearly")) { recurrence ->
                FilterChip(
                    selected = uiState.recurrence == recurrence,
                    onClick = { onRecurrenceChange(recurrence) },
                    label = { Text(recurrence.label()) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (uiState.recurrence != "none") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.recurrenceIntervalText,
                    onValueChange = onRecurrenceIntervalChange,
                    label = { Text("每几次") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.recurrenceUntil,
                    onValueChange = onRecurrenceUntilChange,
                    label = { Text("重复截止") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

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

@Composable
private fun TodoHistorySection(history: List<TodoHistoryEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        Text("历史记录", style = MaterialTheme.typography.titleMedium)
        if (history.isEmpty()) {
            Text(
                text = "暂无历史记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.take(5).forEach { item ->
                Text(
                    text = "${item.createdAt.take(16)}  ${item.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.label(): String {
    return when (this) {
        "daily" -> "每天"
        "weekly" -> "每周"
        "monthly" -> "每月"
        "yearly" -> "每年"
        else -> "不重复"
    }
}
