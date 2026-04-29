package com.example.flux.feature.todo.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.ui.DateTimeField
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.todo.presentation.TodoDetailUiState

@Composable
fun TodoEditableFields(
    uiState: TodoDetailUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onProjectChange: (String?) -> Unit,
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

        DateTimeField(
            value = uiState.startAt,
            onValueChange = onStartAtChange,
            label = "开始",
            modifier = Modifier.fillMaxWidth()
        )
        DateTimeField(
            value = uiState.dueAt,
            onValueChange = onDueAtChange,
            label = "截止",
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.reminderMinutesText,
            onValueChange = onReminderChange,
            label = { Text("提前提醒分钟") },
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

@Composable
fun TodoHistorySection(history: List<TodoHistoryEntity>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
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
                    text = "${TimeUtil.formatTimestampForDisplay(item.createdAt)}  ${item.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
