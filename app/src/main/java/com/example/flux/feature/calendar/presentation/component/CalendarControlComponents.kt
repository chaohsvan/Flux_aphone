package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    onToggleHolidayEditMode: () -> Unit,
    onShowMonthHistory: () -> Unit
) {
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
            TextButton(onClick = onToggleHolidayEditMode, modifier = Modifier.weight(1f)) {
                Text(if (holidayEditMode) "\u5b8c\u6210\u5047\u671f\u6807\u8bb0" else "\u5047\u671f\u6807\u8bb0")
            }
            TextButton(onClick = onShowMonthHistory, modifier = Modifier.weight(1f)) {
                Text("同月记录")
            }
        }

        Text(
            text = if (holidayEditMode) {
                "\u5047\u671f\u6807\u8bb0\u5df2\u5f00\u542f\uff0c\u70b9\u51fb\u65e5\u671f\u53ef\u5728\u5047\u671f / \u5de5\u4f5c\u65e5\u4e4b\u95f4\u5207\u6362"
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

@OptIn(ExperimentalLayoutApi::class)
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

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 3
    ) {
        layerItems.forEach { item ->
            CalendarLayerLegendChip(item)
        }
    }
}

@Composable
private fun CalendarLayerLegendChip(item: CalendarLayerToggleItem) {
    Surface(
        modifier = Modifier.clickable(onClick = item.onClick),
        shape = MaterialTheme.shapes.small,
        color = if (item.selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        contentColor = if (item.selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private data class CalendarLayerToggleItem(
    val label: String,
    val selected: Boolean,
    val color: Color,
    val onClick: () -> Unit
)
