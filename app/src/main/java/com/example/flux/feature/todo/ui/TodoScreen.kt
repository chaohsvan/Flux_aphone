package com.example.flux.feature.todo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.domain.todo.TodoExportFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<TodoExportFormat?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            val format = pendingExportFormat
            if (uri != null && format != null) {
                viewModel.exportSelectedToUri(context, uri, format)
            }
            pendingExportFormat = null
        }
    )

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAllVisible() }) {
                            Text("全选")
                        }
                        TextButton(onClick = { viewModel.invertVisibleSelection() }) {
                            Text("反选")
                        }
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "导出所选")
                        }
                        if (selectedIds.size == 1) {
                            val selectedId = selectedIds.first()
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                            }
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                            }
                        }
                        TextButton(onClick = { viewModel.batchMarkHighPriority() }) {
                            Text("高")
                        }
                        TextButton(onClick = { viewModel.batchMarkNormalPriority() }) {
                            Text("普通")
                        }
                        IconButton(onClick = { viewModel.batchDelete() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除所选",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                Column {
                    TopAppBar(title = { Text("待办事项") })
                    OutlinedTextField(
                        value = filterState.query,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text("搜索待办...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                        trailingIcon = {
                            TextButton(onClick = { showFilterSheet = true }) {
                                Text(
                                    text = "筛选",
                                    color = if (filterState.hasActiveFilters) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增待办")
            }
        }
    ) { paddingValues ->
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (filterState.hasActiveFilters) "没有符合筛选条件的待办" else "暂无待办")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(
                    items = todos,
                    key = { it.id }
                ) { todo ->
                    TodoItemRow(
                        todo = todo,
                        projectName = projects.firstOrNull { it.id == todo.projectId }?.name,
                        isSelected = selectedIds.contains(todo.id),
                        onToggle = { id, currentStatus -> viewModel.toggleStatus(id, currentStatus) },
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                viewModel.toggleSelection(todo.id)
                            } else {
                                onNavigateToDetail(todo.id)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelection(todo.id)
                        }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        TodoFilterSheet(
            filterState = filterState,
            projects = projects,
            onStatusChange = viewModel::setStatusFilter,
            onPriorityChange = viewModel::setPriorityFilter,
            onTimeChange = viewModel::setTimeFilter,
            onProjectChange = viewModel::setProjectFilter,
            onCreateProject = viewModel::addProject,
            onDeleteProject = viewModel::deleteProject,
            onCustomStartDateChange = viewModel::updateCustomStartDate,
            onCustomEndDateChange = viewModel::updateCustomEndDate,
            onClear = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showExportSheet) {
        TodoExportSheet(
            onFormatSelected = { format ->
                pendingExportFormat = format
                exportLauncher.launch("FluxTodos_${com.example.flux.core.util.TimeUtil.getCurrentDate()}.${format.extension}")
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showAddSheet) {
        TodoInputSheet(
            projects = projects,
            onDismiss = { showAddSheet = false },
            onCreateProject = viewModel::addProject,
            onSubmit = { title, desc, priority, projectId ->
                viewModel.addTodo(title, desc, priority, projectId)
                showAddSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoExportSheet(
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
            TextButton(onClick = { onFormatSelected(TodoExportFormat.JSON) }, modifier = Modifier.fillMaxWidth()) {
                Text("JSON")
            }
            TextButton(onClick = { onFormatSelected(TodoExportFormat.CSV) }, modifier = Modifier.fillMaxWidth()) {
                Text("CSV")
            }
            TextButton(onClick = { onFormatSelected(TodoExportFormat.MARKDOWN) }, modifier = Modifier.fillMaxWidth()) {
                Text("Markdown")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoFilterSheet(
    filterState: TodoFilterState,
    projects: List<TodoProjectEntity>,
    onStatusChange: (TodoStatusFilter) -> Unit,
    onPriorityChange: (TodoPriorityFilter) -> Unit,
    onTimeChange: (TodoTimeFilter) -> Unit,
    onProjectChange: (String?) -> Unit,
    onCreateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onCustomStartDateChange: (String) -> Unit,
    onCustomEndDateChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }

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
                TextButton(onClick = onClear) {
                    Text("清除")
                }
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
                item {
                    TodoFilterChip(filterState.projectId == null, "全部") {
                        onProjectChange(null)
                    }
                }
                items(projects, key = { it.id }) { project ->
                    FilterChip(
                        selected = filterState.projectId == project.id,
                        onClick = { onProjectChange(project.id) },
                        label = { Text(project.name) },
                        trailingIcon = {
                            IconButton(onClick = { onDeleteProject(project.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除标签")
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("新项目标签") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                OutlinedTextField(
                    value = filterState.customStartDate,
                    onValueChange = onCustomStartDateChange,
                    label = { Text("开始日期") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filterState.customEndDate,
                    onValueChange = onCustomEndDateChange,
                    label = { Text("结束日期") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
