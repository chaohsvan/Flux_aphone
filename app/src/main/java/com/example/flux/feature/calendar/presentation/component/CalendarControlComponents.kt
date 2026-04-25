package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flux.feature.calendar.presentation.CalendarViewMode
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed

@Composable
fun CalendarControlStrip(
    showDiaries: Boolean,
    showTodos: Boolean,
    showEvents: Boolean,
    showHolidays: Boolean,
    showTrash: Boolean,
    holidayEditMode: Boolean,
    viewMode: CalendarViewMode,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleHolidayEditMode: () -> Unit,
    onShowMonthHistory: () -> Unit
) {
    val activeLayerCount = listOf(showDiaries, showTodos, showEvents, showHolidays, showTrash).count { it }
    val swipeHint = when (viewMode) {
        CalendarViewMode.MONTH -> "左右滑动切换月份"
        CalendarViewMode.DAY -> "左右滑动切换日期"
        CalendarViewMode.WEEK -> "左右滑动切换周"
        CalendarViewMode.QUARTER -> "左右滑动切换季度"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onToggleExpand, modifier = Modifier.weight(1f)) {
                Text(if (isExpanded) "收起图层 | $activeLayerCount" else "图层 | $activeLayerCount")
            }
            TextButton(onClick = onToggleHolidayEditMode, modifier = Modifier.weight(1f)) {
                Text(if (holidayEditMode) "完成日期标记" else "日期标记")
            }
            TextButton(onClick = onShowMonthHistory, modifier = Modifier.weight(1f)) {
                Text("同月记录")
            }
        }

        Text(
            text = if (holidayEditMode) {
                "标记模式已开启，点击日期即可在休 / 班之间切换"
            } else {
                swipeHint
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (holidayEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalendarModeChips(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalendarModeChip("月", viewMode == CalendarViewMode.MONTH, { onViewModeChange(CalendarViewMode.MONTH) }, Modifier.weight(1f))
        CalendarModeChip("日", viewMode == CalendarViewMode.DAY, { onViewModeChange(CalendarViewMode.DAY) }, Modifier.weight(1f))
        CalendarModeChip("周", viewMode == CalendarViewMode.WEEK, { onViewModeChange(CalendarViewMode.WEEK) }, Modifier.weight(1f))
        CalendarModeChip("季", viewMode == CalendarViewMode.QUARTER, { onViewModeChange(CalendarViewMode.QUARTER) }, Modifier.weight(1f))
    }
}

@Composable
fun CalendarModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
}

@Composable
fun CalendarLayerToggles(
    showDiaries: Boolean,
    showTodos: Boolean,
    showEvents: Boolean,
    showHolidays: Boolean,
    showTrash: Boolean,
    onToggleDiaries: () -> Unit,
    onToggleTodos: () -> Unit,
    onToggleEvents: () -> Unit,
    onToggleHolidays: () -> Unit,
    onToggleTrash: () -> Unit
) {
    val layerItems = listOf(
        CalendarLayerToggleItem("日记", showDiaries, FluxDiaryYellow, onToggleDiaries),
        CalendarLayerToggleItem("假期", showHolidays, FluxHolidayOrange, onToggleHolidays),
        CalendarLayerToggleItem("待办", showTodos, FluxTodoRed, onToggleTodos),
        CalendarLayerToggleItem("事件", showEvents, TimelineFallbackColor, onToggleEvents),
        CalendarLayerToggleItem("回收站", showTrash, Color.Gray, onToggleTrash)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(layerItems) { item ->
                FilterChip(
                    selected = item.selected,
                    onClick = item.onClick,
                    label = { Text(item.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                    }
                )
            }
        }
    }
}

private data class CalendarLayerToggleItem(
    val label: String,
    val selected: Boolean,
    val color: Color,
    val onClick: () -> Unit
)
