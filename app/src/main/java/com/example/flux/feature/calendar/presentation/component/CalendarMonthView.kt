package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.feature.calendar.presentation.CalendarMonth
import com.example.flux.feature.calendar.presentation.HolidayOverrideState
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed

@Composable
fun CalendarMonthView(
    currentMonth: CalendarMonth,
    aggregatedData: Map<String, DailyAggregation>,
    showDiaries: Boolean,
    showTodos: Boolean,
    showEvents: Boolean,
    showHolidays: Boolean,
    showTrash: Boolean,
    holidayEditMode: Boolean,
    holidayOverrides: Map<String, HolidayOverrideState>,
    onDateClick: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider()

    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = currentMonth.firstDayOffset()
    val rows = kotlin.math.ceil((startOffset + daysInMonth) / 7.0).toInt()
    val cellCount = rows * 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize()
    ) {
        items((0 until cellCount).toList()) { index ->
            if (index < startOffset || index >= startOffset + daysInMonth) {
                Box(
                    modifier = Modifier
                        .aspectRatio(0.7f)
                        .padding(0.5.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                )
            } else {
                val day = index - startOffset + 1
                val dateString = currentMonth.dateString(day)
                val aggregation = aggregatedData[dateString]
                val defaultHoliday = currentMonth.isWeekend(day)
                val holidayOverride = holidayOverrides[dateString]
                val baseHoliday = holidayOverride?.defaultIsHoliday ?: defaultHoliday
                val isHoliday = holidayOverride?.isHoliday ?: baseHoliday

                CalendarGridCell(
                    day = day,
                    aggregation = aggregation,
                    showDiaries = showDiaries,
                    showTodos = showTodos,
                    showEvents = showEvents,
                    showHolidays = showHolidays,
                    showTrash = showTrash,
                    isHoliday = isHoliday,
                    isHolidayOverride = holidayOverride?.isUserOverride == true,
                    holidayEditMode = holidayEditMode,
                    onClick = { onDateClick(dateString, baseHoliday) }
                )
            }
        }
    }
}

@Composable
fun CalendarGridCell(
    day: Int,
    aggregation: DailyAggregation?,
    showDiaries: Boolean,
    showTodos: Boolean,
    showEvents: Boolean,
    showHolidays: Boolean,
    showTrash: Boolean,
    isHoliday: Boolean,
    isHolidayOverride: Boolean,
    holidayEditMode: Boolean,
    onClick: () -> Unit
) {
    val renderHoliday = showHolidays && isHoliday
    val renderManualWorkday = showHolidays && isHolidayOverride && !isHoliday
    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .padding(0.5.dp)
            .background(
                when {
                    renderHoliday -> FluxHolidayOrange.copy(alpha = 0.14f)
                    holidayEditMode -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (renderHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (renderHoliday) {
                Text(
                    text = if (isHolidayOverride) "休" else "假",
                    style = MaterialTheme.typography.labelSmall,
                    color = FluxHolidayOrange,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (renderManualWorkday) {
                Text(
                    text = "班",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (aggregation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (showDiaries && aggregation.hasDiary) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(FluxDiaryYellow))
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (showTodos && aggregation.pendingTodosCount > 0) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(FluxTodoRed))
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (showTodos && aggregation.completedTodosCount > 0) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray))
                    }
                    if (showEvents && aggregation.eventColors.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(aggregation.eventColors.first())))
                        )
                    }
                    if (showTrash && aggregation.deletedCount > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "删",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
