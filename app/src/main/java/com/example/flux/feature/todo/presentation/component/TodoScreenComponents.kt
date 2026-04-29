package com.example.flux.feature.todo.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.core.ui.DateField
import com.example.flux.feature.todo.domain.TodoFilterState
import com.example.flux.feature.todo.domain.TodoPriorityFilter
import com.example.flux.feature.todo.domain.TodoStats
import com.example.flux.feature.todo.domain.TodoStatusFilter
import com.example.flux.feature.todo.domain.TodoTimeFilter

@Composable
fun TodoStatsRow(stats: TodoStats) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { TodoStatCard("总数", stats.total.toString(), "活跃待办") }
        item { TodoStatCard("今天", stats.today.toString(), "今天截止") }
        item { TodoStatCard("逾期", stats.overdue.toString(), "需要处理", isWarning = stats.overdue > 0) }
        item { TodoStatCard("进行中", stats.inProgress.toString(), "正在推进") }
        item { TodoStatCard("高优先级", stats.highPriority.toString(), "优先关注") }
        item { TodoStatCard("完成率", "${stats.completionPercent}%", "已完成 ${stats.completed}") }
    }
}

@Composable
private fun TodoStatCard(
    title: String,
    value: String,
    subtitle: String,
    isWarning: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .width(116.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoExportSheet(
    onFormatSelected: (TodoExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("导出待办", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { onFormatSelected(TodoExportFormat.JSON) }, modifier = Modifier.fillMaxWidth()) { Text("JSON") }
            TextButton(onClick = { onFormatSelected(TodoExportFormat.CSV) }, modifier = Modifier.fillMaxWidth()) { Text("CSV") }
            TextButton(onClick = { onFormatSelected(TodoExportFormat.MARKDOWN) }, modifier = Modifier.fillMaxWidth()) { Text("Markdown") }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoFilterSheet(
    filterState: TodoFilterState,
    projects: List<TodoProjectEntity>,
    onStatusChange: (TodoStatusFilter) -> Unit,
    onPriorityChange: (TodoPriorityFilter) -> Unit,
    onTimeChange: (TodoTimeFilter) -> Unit,
    onProjectChange: (String?) -> Unit,
    onCustomStartDateChange: (String) -> Unit,
    onCustomEndDateChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("筛选待办", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClear) { Text("清除") }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.status == TodoStatusFilter.ALL, "全部") { onStatusChange(TodoStatusFilter.ALL) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.PENDING, "待办") { onStatusChange(TodoStatusFilter.PENDING) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.IN_PROGRESS, "进行中") { onStatusChange(TodoStatusFilter.IN_PROGRESS) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.COMPLETED, "完成") { onStatusChange(TodoStatusFilter.COMPLETED) } }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("项目标签", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.projectId == null, "全部") { onProjectChange(null) } }
                items(projects, key = { it.id }) { project ->
                    FilterChip(
                        selected = filterState.projectId == project.id,
                        onClick = { onProjectChange(project.id) },
                        label = { Text(project.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("优先级", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.ALL, "全部") { onPriorityChange(TodoPriorityFilter.ALL) } }
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.NORMAL, "普通") { onPriorityChange(TodoPriorityFilter.NORMAL) } }
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.HIGH, "高优先级") { onPriorityChange(TodoPriorityFilter.HIGH) } }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("时间", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.time == TodoTimeFilter.ALL, "全部") { onTimeChange(TodoTimeFilter.ALL) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.TODAY, "今天") { onTimeChange(TodoTimeFilter.TODAY) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.UPCOMING, "近期") { onTimeChange(TodoTimeFilter.UPCOMING) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.OVERDUE, "逾期") { onTimeChange(TodoTimeFilter.OVERDUE) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.SCHEDULED, "定时") { onTimeChange(TodoTimeFilter.SCHEDULED) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.CUSTOM, "自定义") { onTimeChange(TodoTimeFilter.CUSTOM) } }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateField(
                    value = filterState.customStartDate,
                    onValueChange = onCustomStartDateChange,
                    label = "开始日期",
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    value = filterState.customEndDate,
                    onValueChange = onCustomEndDateChange,
                    label = "结束日期",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTagManagementSheet(
    projects: List<TodoProjectEntity>,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }
    var editingProjectId by remember { mutableStateOf<String?>(null) }
    var editingProjectName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("标签管理", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("新标签名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val name = newProjectName.trim()
                        if (name.isNotBlank()) {
                            onCreateProject(name)
                            newProjectName = ""
                        }
                    },
                    enabled = newProjectName.isNotBlank()
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isEmpty()) {
                Text(
                    text = "暂无标签",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                projects.forEach { project ->
                    if (editingProjectId == project.id) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editingProjectName,
                                onValueChange = { editingProjectName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val name = editingProjectName.trim()
                                    if (name.isNotBlank()) {
                                        onRenameProject(project.id, name)
                                        editingProjectId = null
                                        editingProjectName = ""
                                    }
                                },
                                enabled = editingProjectName.isNotBlank()
                            ) {
                                Text("保存")
                            }
                            TextButton(
                                onClick = {
                                    editingProjectId = null
                                    editingProjectName = ""
                                }
                            ) {
                                Text("取消")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        editingProjectId = project.id
                                        editingProjectName = project.name
                                    }
                                ) {
                                    Text("重命名")
                                }
                                TextButton(onClick = { onDeleteProject(project.id) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 8.dp)
    )
}
