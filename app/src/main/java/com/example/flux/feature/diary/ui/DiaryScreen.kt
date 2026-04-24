package com.example.flux.feature.diary.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.domain.diary.DiaryExportFormat
import com.example.flux.core.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onNavigateToEditor: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val diaries by viewModel.diaries.collectAsState()
    val onThisDayDiaries by viewModel.onThisDayDiaries.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isFavoriteFilter by viewModel.isFavoriteFilter.collectAsState()
    val moodFilter by viewModel.moodFilter.collectAsState()
    val monthFilter by viewModel.monthFilter.collectAsState()
    val yearFilter by viewModel.yearFilter.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val diaryTags by viewModel.diaryTags.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    val trashSummary by viewModel.trashSummary.collectAsState()
    val hasFilters = isFavoriteFilter || moodFilter != null || monthFilter != null || yearFilter != null || tagFilter != null

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
                Toast.makeText(context, "\u5df2\u5bfc\u51fa\u65e5\u8bb0", Toast.LENGTH_SHORT).show()
            }
            pendingExportFormat = null
        }
    )

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("\u5df2\u9009\u62e9 ${selectedIds.size} \u7bc7") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "鍙栨秷閫夋嫨")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "\u5bfc\u51fa\u6240\u9009")
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
                    title = { Text("\u65e5\u8bb0") },
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
                                color = if (hasFilters) {
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
            FloatingActionButton(onClick = { onNavigateToEditor("new") }) {
                Icon(Icons.Default.Add, contentDescription = "\u65b0\u5efa\u65e5\u8bb0")
            }
        }
    ) { paddingValues ->
        if (diaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasFilters) {
                        "\u6ca1\u6709\u7b26\u5408\u6761\u4ef6\u7684\u65e5\u8bb0"
                    } else {
                        "\u6682\u65e0\u65e5\u8bb0\uff0c\u5f00\u59cb\u8bb0\u5f55\u751f\u6d3b\u5427\u3002"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (onThisDayDiaries.isNotEmpty()) {
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
        DiaryFilterSheet(
            isFavoriteFilter = isFavoriteFilter,
            moodFilter = moodFilter,
            monthFilter = monthFilter,
            yearFilter = yearFilter,
            tagFilter = tagFilter,
            filterOptions = filterOptions,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryExportSheet(
    onFormatSelected: (DiaryExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("\u5bfc\u51fa\u65e5\u8bb0", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            DiaryExportFormat.entries.forEach { format ->
                TextButton(
                    onClick = { onFormatSelected(format) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (format) {
                            DiaryExportFormat.JSON -> "JSON"
                            DiaryExportFormat.CSV -> "CSV"
                            DiaryExportFormat.MARKDOWN -> "Markdown"
                            DiaryExportFormat.ZIP -> "ZIP"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryFilterSheet(
    isFavoriteFilter: Boolean,
    moodFilter: String?,
    monthFilter: String?,
    yearFilter: String?,
    tagFilter: String?,
    filterOptions: DiaryFilterOptions,
    onFavoriteChange: (Boolean) -> Unit,
    onMoodChange: (String?) -> Unit,
    onMonthChange: (String?) -> Unit,
    onYearChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("\u7b5b\u9009\u65e5\u8bb0", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClear) {
                    Text("娓呴櫎")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("\u6536\u85cf\u72b6\u6001", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = isFavoriteFilter,
                onClick = { onFavoriteChange(!isFavoriteFilter) },
                label = { Text("\u4ec5\u663e\u793a\u5df2\u6536\u85cf") }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u5fc3\u60c5", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val moods = listOf(
                "\u5f00\u5fc3",
                "\u5e73\u9759",
                "\u4f24\u5fc3",
                "\u6124\u6012"
            )
            LazyRow {
                items(moods) { mood ->
                    FilterChip(
                        selected = moodFilter == mood,
                        onClick = {
                            if (moodFilter == mood) onMoodChange(null) else onMoodChange(mood)
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
                                if (tagFilter == tag) onTagChange(null) else onTagChange(tag)
                            },
                            label = { Text("#$tag") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u6708\u4efd", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (filterOptions.months.isEmpty()) {
                Text("\u6682\u65e0\u6708\u4efd\u5f52\u6863", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow {
                    items(filterOptions.months) { month ->
                        FilterChip(
                            selected = monthFilter == month,
                            onClick = {
                                if (monthFilter == month) onMonthChange(null) else onMonthChange(month)
                            },
                            label = { Text(month) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("\u5e74\u4efd", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (filterOptions.years.isEmpty()) {
                Text("\u6682\u65e0\u5e74\u4efd\u5f52\u6863", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow {
                    items(filterOptions.years) { year ->
                        FilterChip(
                            selected = yearFilter == year,
                            onClick = {
                                if (yearFilter == year) onYearChange(null) else onYearChange(year)
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

@Composable
fun OnThisDayBanner(
    diaries: List<DiaryEntity>,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "\u90a3\u5e74\u4eca\u65e5",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp)
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
                            text = diary.entryDate.substring(0, 4) + "\u5e74",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diary.title.ifBlank { diary.contentMd.take(20) + "..." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
