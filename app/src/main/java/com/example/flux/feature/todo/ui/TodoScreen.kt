package com.example.flux.feature.todo.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.domain.todo.TodoExportFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val trashSummary by viewModel.trashSummary.collectAsState()
    val hasStructuredFilters = filterState.status != TodoStatusFilter.ALL ||
        filterState.priority != TodoPriorityFilter.ALL ||
        filterState.time != TodoTimeFilter.ALL ||
        filterState.projectId != null
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<TodoExportFormat?>(null) }
    val listState = rememberLazyListState()
    var draggingTodoId by remember { mutableStateOf<String?>(null) }
    var draggedOffset by remember { mutableStateOf(0f) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            val format = pendingExportFormat
            if (uri != null && format != null) {
                viewModel.exportSelectedToUri(context, uri, format)
                Toast.makeText(context, "\u5df2\u5bfc\u51fa\u5f85\u529e", Toast.LENGTH_SHORT).show()
            }
            pendingExportFormat = null
        }
    )

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("\u5df2\u9009\u62e9 ${selectedIds.size} \u9879") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "鍙栨秷閫夋嫨")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAllVisible() }) {
                            Text("\u5168\u9009")
                        }
                        TextButton(onClick = { viewModel.invertVisibleSelection() }) {
                            Text("\u53cd\u9009")
                        }
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "\u5bfc\u51fa\u6240\u9009")
                        }
                        if (selectedIds.size == 1) {
                            val selectedId = selectedIds.first()
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "涓婄Щ")
                            }
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "涓嬬Щ")
                            }
                        }
                        TextButton(onClick = {
                            viewModel.batchMarkHighPriority()
                            Toast.makeText(context, "\u5df2\u8bbe\u4e3a\u9ad8\u4f18\u5148\u7ea7", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("\u9ad8\u4f18")
                        }
                        TextButton(onClick = {
                            viewModel.batchMarkNormalPriority()
                            Toast.makeText(context, "\u5df2\u8bbe\u4e3a\u666e\u901a\u4f18\u5148\u7ea7", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("\u666e\u901a")
                        }
                        IconButton(onClick = {
                            viewModel.batchDelete()
                            Toast.makeText(context, "\u5df2\u79fb\u5165\u56de\u6536\u7ad9", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "\u5220\u9664\u6240\u9009",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("\u5f85\u529e\u4e8b\u9879") },
                    actions = {
                        IconButton(onClick = onOpenGlobalSearch) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "\u7edf\u4e00\u641c\u7d22"
                            )
                        }
                        TextButton(onClick = { showFilterSheet = true }) {
                            Text(
                                text = "\u7b5b\u9009",
                                color = if (hasStructuredFilters) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        TextButton(onClick = onNavigateToTrash) {
                            Text(if (trashSummary.total > 0) "\u56de\u6536\u7ad9 ${trashSummary.total}" else "\u56de\u6536\u7ad9")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "\u65b0\u589e\u5f85\u529e")
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
                Text(
                    text = if (filterState.hasActiveFilters) {
                        "\u6ca1\u6709\u7b26\u5408\u7b5b\u9009\u6761\u4ef6\u7684\u5f85\u529e"
                    } else {
                        "\u6682\u65e0\u5f85\u529e"
                    }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item(key = "todo_stats") {
                    TodoStatsRow(stats = stats)
                }
                itemsIndexed(
                    items = todos,
                    key = { _, todo -> todo.id }
                ) { _, todo ->
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
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                val isDragging = draggingTodoId == todo.id
                                translationY = if (isDragging) draggedOffset else 0f
                                alpha = if (isDragging) 0.92f else 1f
                            }
                            .pointerInput(
                                todo.id,
                                todos,
                                selectedIds.isEmpty(),
                                filterState.hasActiveFilters
                            ) {
                                if (selectedIds.isEmpty() && !filterState.hasActiveFilters) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingTodoId = todo.id
                                            draggedOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggingTodoId = null
                                            draggedOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggingTodoId = null
                                            draggedOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedOffset += dragAmount.y

                                            val currentItem = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == todo.id }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val draggedCenter = currentItem.offset + currentItem.size / 2f + draggedOffset
                                            val targetItem = listState.layoutInfo.visibleItemsInfo
                                                .filter { it.key != "todo_stats" && it.key != todo.id }
                                                .firstOrNull { item ->
                                                    draggedCenter >= item.offset &&
                                                        draggedCenter <= item.offset + item.size
                                                }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val targetIndex = targetItem.index - 1
                                            if (targetIndex in todos.indices) {
                                                viewModel.moveTodoToIndex(todo.id, targetIndex)
                                                draggedOffset = 0f
                                            }
                                        }
                                    )
                                }
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
            onSubmit = { title, desc, priority, projectId, dueAt ->
                viewModel.addTodo(title, desc, priority, projectId, dueAt)
                Toast.makeText(context, "\u5df2\u521b\u5efa\u5f85\u529e", Toast.LENGTH_SHORT).show()
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun TodoStatsRow(stats: TodoStats) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { TodoStatCard("\u603b\u6570", stats.total.toString(), "\u6d3b\u8dc3\u5f85\u529e") }
        item { TodoStatCard("\u4eca\u5929", stats.today.toString(), "\u4eca\u5929\u622a\u6b62") }
        item { TodoStatCard("\u903e\u671f", stats.overdue.toString(), "\u9700\u8981\u5904\u7406", isWarning = stats.overdue > 0) }
        item { TodoStatCard("\u8fdb\u884c\u4e2d", stats.inProgress.toString(), "\u6b63\u5728\u63a8\u8fdb") }
        item { TodoStatCard("\u9ad8\u4f18\u5148\u7ea7", stats.highPriority.toString(), "\u4f18\u5148\u5173\u6ce8") }
        item { TodoStatCard("\u5b8c\u6210\u7387", "${stats.completionPercent}%", "\u5df2\u5b8c\u6210 ${stats.completed}") }
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
            containerColor = if (isWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .width(116.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isWarning) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isWarning) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
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
            Text("\u5bfc\u51fa\u5f85\u529e", style = MaterialTheme.typography.titleLarge)
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
                Text("\u7b5b\u9009\u5f85\u529e", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClear) {
                    Text("\u6e05\u9664")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("\u72b6\u6001", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.status == TodoStatusFilter.ALL, "\u5168\u90e8") { onStatusChange(TodoStatusFilter.ALL) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.PENDING, "\u5f85\u529e") { onStatusChange(TodoStatusFilter.PENDING) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.IN_PROGRESS, "\u8fdb\u884c\u4e2d") { onStatusChange(TodoStatusFilter.IN_PROGRESS) } }
                item { TodoFilterChip(filterState.status == TodoStatusFilter.COMPLETED, "\u5b8c\u6210") { onStatusChange(TodoStatusFilter.COMPLETED) } }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u9879\u76ee\u6807\u7b7e", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item {
                    TodoFilterChip(filterState.projectId == null, "\u5168\u90e8") {
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
                                Icon(Icons.Default.Close, contentDescription = "\u5220\u9664\u6807\u7b7e")
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
                    label = { Text("\u65b0\u9879\u76ee\u540d\u79f0") },
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
                    Text("\u6dfb\u52a0")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u4f18\u5148\u7ea7", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.ALL, "\u5168\u90e8") { onPriorityChange(TodoPriorityFilter.ALL) } }
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.NORMAL, "\u666e\u901a") { onPriorityChange(TodoPriorityFilter.NORMAL) } }
                item { TodoFilterChip(filterState.priority == TodoPriorityFilter.HIGH, "\u9ad8\u4f18\u5148\u7ea7") { onPriorityChange(TodoPriorityFilter.HIGH) } }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u65f6\u95f4", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                item { TodoFilterChip(filterState.time == TodoTimeFilter.ALL, "\u5168\u90e8") { onTimeChange(TodoTimeFilter.ALL) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.TODAY, "\u4eca\u5929") { onTimeChange(TodoTimeFilter.TODAY) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.UPCOMING, "\u8fd1\u671f") { onTimeChange(TodoTimeFilter.UPCOMING) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.OVERDUE, "\u903e\u671f") { onTimeChange(TodoTimeFilter.OVERDUE) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.SCHEDULED, "\u5b9a\u65f6") { onTimeChange(TodoTimeFilter.SCHEDULED) } }
                item { TodoFilterChip(filterState.time == TodoTimeFilter.CUSTOM, "\u81ea\u5b9a\u4e49") { onTimeChange(TodoTimeFilter.CUSTOM) } }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.customStartDate,
                    onValueChange = onCustomStartDateChange,
                    label = { Text("\u5f00\u59cb\u65e5\u671f") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filterState.customEndDate,
                    onValueChange = onCustomEndDateChange,
                    label = { Text("\u7ed3\u675f\u65e5\u671f") },
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
