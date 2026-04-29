package com.example.flux.feature.todo.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.ui.DateTimeField
import com.example.flux.core.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoInputSheet(
    projects: List<TodoProjectEntity>,
    initialDueAt: String = "",
    onDismiss: () -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSubmit: (String, String, String, String?, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf(initialDueAt) }
    var isHighPriority by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var showTagManagement by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var editingProjectId by remember { mutableStateOf<String?>(null) }
    var editingProjectName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "新建待办",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务内容") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            DateTimeField(
                value = dueAt,
                onValueChange = { dueAt = it; errorMessage = null },
                label = "截止日期",
                modifier = Modifier.fillMaxWidth(),
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("项目标签", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showTagManagement = !showTagManagement }) {
                    Text(if (showTagManagement) "完成" else "管理标签")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item {
                    FilterChip(
                        selected = selectedProjectId == null,
                        onClick = { selectedProjectId = null },
                        label = { Text("无标签") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                items(projects, key = { it.id }) { project ->
                    FilterChip(
                        selected = selectedProjectId == project.id,
                        onClick = { selectedProjectId = project.id },
                        label = { Text(project.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (showTagManagement) {
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

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                projects.forEach { project ->
                    if (editingProjectId == project.id) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                TextButton(
                                    onClick = {
                                        if (selectedProjectId == project.id) {
                                            selectedProjectId = null
                                        }
                                        onDeleteProject(project.id)
                                    }
                                ) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isHighPriority,
                    onCheckedChange = { isHighPriority = it }
                )
                Text("设为高优先级")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (!TimeUtil.isValidDateOrDateTime(dueAt.trim())) {
                            errorMessage = "截止日期格式应为 YYYY-MM-DD 或 YYYY-MM-DD HH:mm"
                        } else {
                            onSubmit(
                                title.trim(),
                                description.trim(),
                                if (isHighPriority) "high" else "normal",
                                selectedProjectId,
                                dueAt.trim()
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}
