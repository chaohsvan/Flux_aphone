package com.example.flux.feature.todo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(title = { Text("待办事项") })
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) { paddingValues ->
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "所有任务均已完成，或者暂无任务。")
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

    if (showAddSheet) {
        TodoInputSheet(
            onDismiss = { showAddSheet = false },
            onSubmit = { title, desc, priority ->
                viewModel.addTodo(title, desc, priority)
                showAddSheet = false
            }
        )
    }
}
