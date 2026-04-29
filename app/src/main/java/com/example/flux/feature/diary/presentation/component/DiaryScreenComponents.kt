package com.example.flux.feature.diary.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.domain.diary.DiaryExportFormat
import com.example.flux.feature.diary.presentation.DiaryUiState

@Composable
fun DiaryList(
    uiState: DiaryUiState,
    paddingValues: PaddingValues,
    onNavigateToEditor: (String) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    ) {
        if (uiState.onThisDayDiaries.isNotEmpty()) {
            item {
                OnThisDayBanner(
                    diaries = uiState.onThisDayDiaries,
                    onClick = onNavigateToEditor
                )
            }
        }

        items(
            items = uiState.diaries,
            key = { it.id }
        ) { diary ->
            DiaryItemRow(
                diary = diary,
                tags = uiState.diaryTags[diary.id].orEmpty(),
                isSelected = uiState.selectedIds.contains(diary.id),
                onClick = {
                    if (uiState.selectedIds.isNotEmpty()) {
                        onToggleSelection(diary.id)
                    } else {
                        onNavigateToEditor(it)
                    }
                },
                onLongClick = { onToggleSelection(diary.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryExportSheet(
    onFormatSelected: (DiaryExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("导出日记", style = MaterialTheme.typography.titleLarge)
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
fun DiaryFilterSheet(
    uiState: DiaryUiState,
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
                Text("筛选日记", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClear) {
                    Text("清除")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("收藏状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = uiState.isFavoriteFilter,
                onClick = { onFavoriteChange(!uiState.isFavoriteFilter) },
                label = { Text("仅显示已收藏") }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("心情", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val moods = DiaryMoodOptions
            LazyRow {
                items(moods) { mood ->
                    FilterChip(
                        selected = uiState.moodFilter == mood,
                        onClick = {
                            if (uiState.moodFilter == mood) onMoodChange(null) else onMoodChange(mood)
                        },
                        label = { Text(mood) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("标签", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.filterOptions.tags.isEmpty()) {
                Text("暂无标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow {
                    items(uiState.filterOptions.tags) { tag ->
                        FilterChip(
                            selected = uiState.tagFilter == tag,
                            onClick = {
                                if (uiState.tagFilter == tag) onTagChange(null) else onTagChange(tag)
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
            if (uiState.filterOptions.months.isEmpty()) {
                Text("暂无月份归档", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow {
                    items(uiState.filterOptions.months) { month ->
                        FilterChip(
                            selected = uiState.monthFilter == month,
                            onClick = {
                                if (uiState.monthFilter == month) onMonthChange(null) else onMonthChange(month)
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
            if (uiState.filterOptions.years.isEmpty()) {
                Text("暂无年份归档", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow {
                    items(uiState.filterOptions.years) { year ->
                        FilterChip(
                            selected = uiState.yearFilter == year,
                            onClick = {
                                if (uiState.yearFilter == year) onYearChange(null) else onYearChange(year)
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
            text = "那年今日",
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
                            text = diary.entryDate.substring(0, 4) + "年",
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
