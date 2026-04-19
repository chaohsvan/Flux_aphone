package com.example.flux.feature.diary.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onNavigateToEditor: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val diaries by viewModel.diaries.collectAsState()
    val onThisDayDiaries by viewModel.onThisDayDiaries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isFavoriteFilter by viewModel.isFavoriteFilter.collectAsState()
    val moodFilter by viewModel.moodFilter.collectAsState()
    val monthFilter by viewModel.monthFilter.collectAsState()
    val yearFilter by viewModel.yearFilter.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val diaryTags by viewModel.diaryTags.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportSelectedToUri(context, uri)
            }
        }
    )

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 篇") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            exportLauncher.launch("FluxExport_${com.example.flux.core.util.TimeUtil.getCurrentDate()}.zip")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Selected")
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
                Column {
                    TopAppBar(
                        title = { Text("日记") },
                        actions = {
                            IconButton(onClick = onNavigateToTrash) {
                                Icon(Icons.Default.Delete, contentDescription = "回收站")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("搜索日记 (FTS5)...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Text(
                                    text = "筛选",
                                    color = if (isFavoriteFilter || moodFilter != null || monthFilter != null || yearFilter != null || tagFilter != null) {
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
            FloatingActionButton(onClick = { onNavigateToEditor("new") }) {
                Icon(Icons.Default.Add, contentDescription = "新建日记")
            }
        }
    ) { paddingValues ->
        if (diaries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "暂无日记，开始记录生活吧。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (onThisDayDiaries.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        OnThisDayBanner(
                            diaries = onThisDayDiaries,
                            onClick = onNavigateToEditor
                        )
                    }
                }

                items(
                    items = diaries,
                    key = { it.id }
                ) { diary ->
                    DiaryItemRow(
                        diary = diary,
                        tags = diaryTags[diary.id].orEmpty(),
                        isSelected = selectedIds.contains(diary.id),
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                viewModel.toggleSelection(diary.id)
                            } else {
                                onNavigateToEditor(it)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelection(diary.id)
                        }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text("高级筛选", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("清除")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("收藏状态", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = isFavoriteFilter,
                    onClick = { viewModel.setFavoriteFilter(!isFavoriteFilter) },
                    label = { Text("仅显示已收藏") }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("心情", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val moods = listOf("开心", "平静", "伤心", "愤怒")
                LazyRow {
                    items(moods) { mood ->
                        FilterChip(
                            selected = moodFilter == mood,
                            onClick = { 
                                if (moodFilter == mood) viewModel.setMoodFilter(null)
                                else viewModel.setMoodFilter(mood)
                            },
                            label = { Text(mood) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("\u6807\u7b7e", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (filterOptions.tags.isEmpty()) {
                    Text("\u6682\u65e0\u6807\u7b7e", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow {
                        items(filterOptions.tags) { tag ->
                            FilterChip(
                                selected = tagFilter == tag,
                                onClick = {
                                    if (tagFilter == tag) viewModel.setTagFilter(null)
                                    else viewModel.setTagFilter(tag)
                                },
                                label = { Text("#$tag") },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("月份", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (filterOptions.months.isEmpty()) {
                    Text("暂无月份归档", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow {
                        items(filterOptions.months) { month ->
                            FilterChip(
                                selected = monthFilter == month,
                                onClick = {
                                    if (monthFilter == month) viewModel.setMonthFilter(null)
                                    else viewModel.setMonthFilter(month)
                                },
                                label = { Text(month) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("年份", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (filterOptions.years.isEmpty()) {
                    Text("暂无年份归档", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow {
                        items(filterOptions.years) { year ->
                            FilterChip(
                                selected = yearFilter == year,
                                onClick = {
                                    if (yearFilter == year) viewModel.setYearFilter(null)
                                    else viewModel.setYearFilter(year)
                                },
                                label = { Text(year) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun OnThisDayBanner(
    diaries: List<com.example.flux.core.database.entity.DiaryEntity>,
    onClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "那年今日",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(diaries) { diary ->
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(end = 8.dp)
                        .clickable { onClick(diary.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = diary.entryDate.substring(0, 4) + "年",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diary.title.ifBlank { diary.contentMd.take(20) + "..." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
