package com.example.flux.feature.trash.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAttachmentManager: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "\u65e5\u8bb0 ${uiState.deletedDiaries.size}",
        "\u5f85\u529e ${uiState.deletedTodos.size}",
        "\u4e8b\u4ef6 ${uiState.deletedEvents.size}"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u56de\u6536\u7ad9") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u8fd4\u56de")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAttachmentManager) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "\u9644\u4ef6\u7ba1\u7406",
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
            TrashSummaryCard(
                diaryCount = uiState.deletedDiaries.size,
                todoCount = uiState.deletedTodos.size,
                eventCount = uiState.deletedEvents.size
            )

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
                0 -> TrashList(
                    hint = "\u6062\u590d\u65e5\u8bb0\u65f6\uff0c\u5982\u679c\u5f53\u5929\u5df2\u7ecf\u6709\u6d3b\u52a8\u65e5\u8bb0\uff0c\u4f1a\u81ea\u52a8\u5408\u5e76\u5185\u5bb9\u548c\u6807\u7b7e\u3002",
                    isEmpty = uiState.deletedDiaries.isEmpty(),
                    emptyText = "\u6ca1\u6709\u5df2\u5220\u9664\u7684\u65e5\u8bb0",
                    items = uiState.deletedDiaries,
                    key = { it.id }
                ) { diary ->
                    TrashEntryRow(
                        title = diary.title.ifBlank { diary.entryDate },
                        subtitle = buildString {
                            append(diary.entryDate)
                            diary.entryTime?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                        },
                        onRestore = {
                            viewModel.restoreDiary(diary.id)
                            Toast.makeText(context, "\u5df2\u6062\u590d\u65e5\u8bb0", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                1 -> TrashList(
                    hint = "\u6062\u590d\u540e\u7684\u5f85\u529e\u4f1a\u4fdd\u7559\u5386\u53f2\u8bb0\u5f55\uff0c\u5e76\u56de\u5230\u539f\u6709\u9879\u76ee\u548c\u6392\u5e8f\u4f53\u7cfb\u3002",
                    isEmpty = uiState.deletedTodos.isEmpty(),
                    emptyText = "\u6ca1\u6709\u5df2\u5220\u9664\u7684\u5f85\u529e",
                    items = uiState.deletedTodos,
                    key = { it.id }
                ) { todo ->
                    TrashEntryRow(
                        title = todo.title,
                        subtitle = todo.dueAt ?: todo.createdAt,
                        onRestore = {
                            viewModel.restoreTodo(todo.id)
                            Toast.makeText(context, "\u5df2\u6062\u590d\u5f85\u529e", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                else -> TrashList(
                    hint = "\u6062\u590d\u540e\u7684\u4e8b\u4ef6\u4f1a\u91cd\u65b0\u51fa\u73b0\u5728\u5bf9\u5e94\u65e5\u671f\u548c\u4e8b\u4ef6\u89c6\u56fe\u4e2d\u3002",
                    isEmpty = uiState.deletedEvents.isEmpty(),
                    emptyText = "\u6ca1\u6709\u5df2\u5220\u9664\u7684\u4e8b\u4ef6",
                    items = uiState.deletedEvents,
                    key = { it.id }
                ) { event ->
                    TrashEntryRow(
                        title = event.title,
                        subtitle = event.startAt,
                        onRestore = {
                            viewModel.restoreEvent(event.id)
                            Toast.makeText(context, "\u5df2\u6062\u590d\u4e8b\u4ef6", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashSummaryCard(
    diaryCount: Int,
    todoCount: Int,
    eventCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "\u5f53\u524d\u5171\u6709 ${diaryCount + todoCount + eventCount} \u9879\u53ef\u6062\u590d\u5185\u5bb9",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "\u65e5\u8bb0 $diaryCount | \u5f85\u529e $todoCount | \u4e8b\u4ef6 $eventCount",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> TrashList(
    hint: String,
    isEmpty: Boolean,
    emptyText: String,
    items: List<T>,
    key: (T) -> Any,
    rowContent: @Composable (T) -> Unit
) {
    if (isEmpty) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = hint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(items, key = key) { item ->
            rowContent(item)
        }
    }
}

@Composable
private fun TrashEntryRow(
    title: String,
    subtitle: String,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onRestore) {
            Text("\u6062\u590d")
        }
    }
}
