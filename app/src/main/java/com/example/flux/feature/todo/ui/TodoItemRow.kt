package com.example.flux.feature.todo.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.ui.theme.FluxTodoRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoItemRow(
    todo: TodoEntity,
    isSelected: Boolean = false,
    onToggle: (String, String) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCompleted = todo.status == "completed"
    val isHighPriority = todo.priority == "high"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle(todo.id, todo.status) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant 
                            else if (isHighPriority) FluxTodoRed 
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TodoMetaLine(todo)
            }
        }
    }
}

@Composable
private fun TodoMetaLine(todo: TodoEntity) {
    val metadata = listOfNotNull(
        todo.startAt?.let { "开始 $it" },
        todo.dueAt?.let { "截止 $it" },
        todo.reminderMinutes?.let { "提前 ${it} 分钟提醒" }
    )

    if (metadata.isNotEmpty()) {
        Text(
            text = metadata.joinToString("  "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
