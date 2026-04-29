package com.example.flux.feature.todo.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.todo.presentation.component.TodoExportSheet
import com.example.flux.feature.todo.presentation.component.TodoFilterSheet
import com.example.flux.feature.todo.presentation.component.TodoInputSheet
import com.example.flux.feature.todo.presentation.component.TodoItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
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
                Toast.makeText(context, "已导出待办", Toast.LENGTH_SHORT).show()
            }
            pendingExportFormat = null
        }
    )

    Scaffold(
        topBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("已选择 ${uiState.selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        if (uiState.selectedIds.size == 1) {
                            val selectedId = uiState.selectedIds.first()
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                            }
                            IconButton(onClick = { viewModel.moveTodoOrder(selectedId, moveUp = false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                            }
                        }
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "导出所选")
                        }
                        IconButton(onClick = {
                            viewModel.batchDelete()
                            Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除所选", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { showSelectionMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(
                            expanded = showSelectionMenu,
                            onDismissRequest = { showSelectionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全选当前列表") },
                                onClick = {
                                    showSelectionMenu = false
                                    viewModel.selectAllVisible()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("反选当前列表") },
                                onClick = {
                                    showSelectionMenu = false
                                    viewModel.invertVisibleSelection()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("标为高优先级") },
                                onClick = {
                                    showSelectionMenu = false
                                    viewModel.batchMarkHighPriority()
                                    Toast.makeText(context, "已设为高优先级", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("标为普通优先级") },
                                onClick = {
                                    showSelectionMenu = false
                                    viewModel.batchMarkNormalPriority()
                                    Toast.makeText(context, "已设为普通优先级", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("待办事项") },
                    actions = {
                        IconButton(onClick = onOpenGlobalSearch) {
                            Icon(Icons.Default.Search, contentDescription = "全局搜索")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "筛选",
                                tint = if (uiState.hasStructuredFilters) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                Icons.Default.Recycling,
                                contentDescription = if (uiState.trashSummary.total > 0) {
                                    "回收站 ${uiState.trashSummary.total}"
                                } else {
                                    "回收站"
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增待办")
            }
        }
    ) { paddingValues ->
        if (uiState.todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.filterState.hasActiveFilters) {
                        "没有符合筛选条件的待办"
                    } else {
                        "暂无待办"
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
                itemsIndexed(
                    items = uiState.todos,
                    key = { _, todo -> todo.id }
                ) { _, todo ->
                    TodoItemRow(
                        todo = todo,
                        projectName = uiState.projects.firstOrNull { it.id == todo.projectId }?.name,
                        subtaskProgress = uiState.subtaskProgress[todo.id],
                        isSelected = uiState.selectedIds.contains(todo.id),
                        onToggle = { id, currentStatus -> viewModel.toggleStatus(id, currentStatus) },
                        onClick = {
                            if (uiState.selectedIds.isNotEmpty()) {
                                viewModel.toggleSelection(todo.id)
                            } else {
                                onNavigateToDetail(todo.id)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(todo.id) },
                        modifier = Modifier
                            .graphicsLayer {
                                val isDragging = draggingTodoId == todo.id
                                translationY = if (isDragging) draggedOffset else 0f
                                alpha = if (isDragging) 0.92f else 1f
                            }
                            .pointerInput(
                                todo.id,
                                uiState.todos,
                                uiState.selectedIds.isEmpty(),
                                uiState.filterState.hasActiveFilters
                            ) {
                                if (uiState.selectedIds.isEmpty() && !uiState.filterState.hasActiveFilters) {
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
                                                .filter { it.key != todo.id }
                                                .firstOrNull { item ->
                                                    draggedCenter >= item.offset &&
                                                        draggedCenter <= item.offset + item.size
                                                }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val targetIndex = targetItem.index
                                            if (targetIndex in uiState.todos.indices) {
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
            filterState = uiState.filterState,
            projects = uiState.projects,
            onStatusChange = viewModel::setStatusFilter,
            onPriorityChange = viewModel::setPriorityFilter,
            onTimeChange = viewModel::setTimeFilter,
            onProjectChange = viewModel::setProjectFilter,
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
                exportLauncher.launch("FluxTodos_${TimeUtil.getCurrentDate()}.${format.extension}")
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showAddSheet) {
        TodoInputSheet(
            projects = uiState.projects,
            onDismiss = { showAddSheet = false },
            onCreateProject = viewModel::addProject,
            onRenameProject = viewModel::renameProject,
            onDeleteProject = viewModel::deleteProject,
            onSubmit = { title, desc, priority, projectId, dueAt ->
                viewModel.addTodo(title, desc, priority, projectId, dueAt)
                Toast.makeText(context, "已创建待办", Toast.LENGTH_SHORT).show()
                showAddSheet = false
            }
        )
    }
}
