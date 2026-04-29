package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.flux.core.util.TimeUtil
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
    weekStartDay: Int,
    holidayOverrides: Map<String, HolidayOverrideState>,
    onDateClick: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            calendarWeekdayLabels(weekStartDay).forEach { day ->
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
        val startOffset = currentMonth.firstDayOffset(weekStartDay)
        val rows = kotlin.math.ceil((startOffset + daysInMonth) / 7.0).toInt()
        val cellCount = rows * 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items((0 until cellCount).toList()) { index ->
                if (index < startOffset || index >= startOffset + daysInMonth) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.82f)
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
                        isToday = dateString == TimeUtil.getCurrentDate(),
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
}

@Composable
fun CalendarGridCell(
    day: Int,
    isToday: Boolean,
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
            .aspectRatio(0.82f)
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
                color = when {
                    renderHoliday -> FluxHolidayOrange
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .then(
                        if (isToday) {
                            Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        } else {
                            Modifier
                        }
                    )
            )

            if (isToday) {
                Text(
                    text = "\u4eca\u5929",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (renderManualWorkday) {
                Text(
                    text = "\u5de5\u4f5c",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (aggregation != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showDiaries && aggregation.hasDiary) {
                        CalendarContentBar(color = FluxDiaryYellow)
                    }
                    if (showTodos && aggregation.pendingTodosCount > 0) {
                        CalendarContentBar(color = FluxTodoRed)
                    }
                    if (showTodos && aggregation.completedTodosCount > 0) {
                        CalendarContentBar(color = Color.Gray)
                    }
                    if (showEvents && aggregation.eventColors.isNotEmpty()) {
                        CalendarContentBar(color = Color(android.graphics.Color.parseColor(aggregation.eventColors.first())))
                    }
                    if (showTrash && aggregation.deletedCount > 0) {
                        CalendarContentBar(color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarContentBar(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.58f)
            .height(3.dp)
            .background(color, shape = MaterialTheme.shapes.extraSmall)
    )
}
