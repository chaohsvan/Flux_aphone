package com.example.flux.feature.trash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.feature.diary.ui.DiaryItemRow
import com.example.flux.feature.todo.ui.TodoItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAttachmentManager: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedDiaries by viewModel.deletedDiaries.collectAsState()
    val deletedTodos by viewModel.deletedTodos.collectAsState()
    val deletedEvents by viewModel.deletedEvents.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("日记", "待办", "事件")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站（点击恢复）") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAttachmentManager) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "附件清理",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    if (deletedDiaries.isEmpty()) {
                        EmptyTrashText("没有已删除的日记")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(deletedDiaries, key = { it.id }) { diary ->
                                DiaryItemRow(
                                    diary = diary,
                                    onClick = { viewModel.restoreDiary(diary.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (deletedTodos.isEmpty()) {
                        EmptyTrashText("没有已删除的待办")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(deletedTodos, key = { it.id }) { todo ->
                                TodoItemRow(
                                    todo = todo,
                                    onToggle = { _, _ -> },
                                    onClick = { viewModel.restoreTodo(todo.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (deletedEvents.isEmpty()) {
                        EmptyTrashText("没有已删除的事件")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(deletedEvents, key = { it.id }) { event ->
                                ListItem(
                                    headlineContent = { Text(event.title) },
                                    supportingContent = { Text(event.startAt) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.restoreEvent(event.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTrashText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
