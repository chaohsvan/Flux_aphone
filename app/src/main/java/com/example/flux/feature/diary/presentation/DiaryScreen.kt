package com.example.flux.feature.diary.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.domain.diary.DiaryExportFormat
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.diary.presentation.component.DiaryExportSheet
import com.example.flux.feature.diary.presentation.component.DiaryFilterSheet
import com.example.flux.feature.diary.presentation.component.DiaryList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onNavigateToEditor: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<DiaryExportFormat?>(null) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            val format = pendingExportFormat
            if (uri != null && format != null) {
                viewModel.exportSelectedToUri(context, uri, format)
                Toast.makeText(context, "已导出日记", Toast.LENGTH_SHORT).show()
            }
            pendingExportFormat = null
        }
    )

    Scaffold(
        topBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("已选择 ${uiState.selectedIds.size} 篇") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "导出所选")
                        }
                        IconButton(onClick = {
                            viewModel.batchDelete()
                            Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
                        }) {
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
                TopAppBar(
                    title = { Text("日记") },
                    actions = {
                        IconButton(onClick = onOpenGlobalSearch) {
                            Icon(Icons.Default.Search, contentDescription = "全局搜索")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "筛选",
                                tint = if (uiState.hasFilters) {
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
            FloatingActionButton(onClick = { onNavigateToEditor("new") }) {
                Icon(Icons.Default.Add, contentDescription = "新建日记")
            }
        }
    ) { paddingValues ->
        if (uiState.diaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.hasFilters) {
                        "没有符合条件的日记"
                    } else {
                        "暂无日记，开始记录生活吧。"
                    }
                )
            }
        } else {
            DiaryList(
                uiState = uiState,
                paddingValues = paddingValues,
                onNavigateToEditor = onNavigateToEditor,
                onToggleSelection = viewModel::toggleSelection
            )
        }
    }

    if (showFilterSheet) {
        DiaryFilterSheet(
            uiState = uiState,
            onFavoriteChange = viewModel::setFavoriteFilter,
            onMoodChange = viewModel::setMoodFilter,
            onMonthChange = viewModel::setMonthFilter,
            onYearChange = viewModel::setYearFilter,
            onTagChange = viewModel::setTagFilter,
            onClear = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showExportSheet) {
        DiaryExportSheet(
            onFormatSelected = { format ->
                pendingExportFormat = format
                exportLauncher.launch("FluxDiaries_${TimeUtil.getCurrentDate()}.${format.extension}")
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
    }
}
