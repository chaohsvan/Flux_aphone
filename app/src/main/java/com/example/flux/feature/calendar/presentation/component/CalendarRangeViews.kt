package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun CalendarQuarterView(
    currentMonth: CalendarMonth,
    aggregatedData: Map<String, DailyAggregation>,
    showEvents: Boolean,
    showHolidays: Boolean,
    weekStartDay: Int,
    holidayOverrides: Map<String, HolidayOverrideState>,
    onDateClick: (String) -> Unit
) {
    val months = remember(currentMonth) { currentMonth.quarterMonths() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(months) { month ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(month.label, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    calendarWeekdayLabels(weekStartDay).forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val daysInMonth = month.lengthOfMonth()
                val startOffset = month.firstDayOffset(weekStartDay)
                val cellCount = kotlin.math.ceil((startOffset + daysInMonth) / 7.0).toInt() * 7
                (0 until cellCount step 7).forEach { rowStart ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        (rowStart until rowStart + 7).forEach { index ->
                            if (index < startOffset || index >= startOffset + daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(64.dp))
                            } else {
                                val day = index - startOffset + 1
                                val date = month.dateString(day)
                                val holidayState = holidayOverrides[date]
                                val isHoliday = holidayState?.isHoliday ?: date.isWeekendDate()
                                QuarterDayCell(
                                    day = day,
                                    aggregation = aggregatedData[date],
                                    showEvents = showEvents,
                                    showHoliday = showHolidays && isHoliday,
                                    holidayLabel = holidayState?.label ?: if (isHoliday) "休" else null,
                                    onClick = { onDateClick(date) },
                                    modifier = Modifier.weight(1f)
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
fun QuarterDayCell(
    day: Int,
    aggregation: DailyAggregation?,
    showEvents: Boolean,
    showHoliday: Boolean,
    holidayLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(aggregation, showEvents) {
        buildList {
            if (aggregation?.hasDiary == true) add("记")
            val pendingCount = aggregation?.pendingTodosCount ?: 0
            if (pendingCount > 0) add("待$pendingCount")
            val eventCount = aggregation?.eventColors?.size ?: 0
            if (showEvents && eventCount > 0) add("事$eventCount")
            val deletedCount = aggregation?.deletedCount ?: 0
            if (deletedCount > 0) add("删$deletedCount")
        }.joinToString(" ")
    }

    Column(
        modifier = modifier
            .height(64.dp)
            .background(if (showHoliday) FluxHolidayOrange.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (showHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurface
        )
        if (showHoliday && holidayLabel != null) {
            Text(
                text = holidayLabel.take(2),
                style = MaterialTheme.typography.labelSmall,
                color = FluxHolidayOrange,
                maxLines = 1
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (aggregation?.hasDiary == true) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(FluxDiaryYellow))
            }
            if ((aggregation?.pendingTodosCount ?: 0) > 0) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(FluxTodoRed))
            }
            if (showEvents && aggregation?.eventColors?.isNotEmpty() == true) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(aggregation.eventColors.first())))
                )
            }
        }
        if (summary.isNotBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun CalendarWeekView(
    selectedDate: String,
    aggregatedData: Map<String, DailyAggregation>,
    showEvents: Boolean,
    showHolidays: Boolean,
    weekStartDay: Int,
    holidayOverrides: Map<String, HolidayOverrideState>,
    onDateClick: (String) -> Unit
) {
    val weekDates = remember(selectedDate, weekStartDay) { selectedDate.weekDates(weekStartDay) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weekDates) { date ->
            val aggregation = aggregatedData[date]
            val holidayState = holidayOverrides[date]
            val isHoliday = holidayState?.isHoliday ?: date.isWeekendDate()
            val holidayLabel = holidayState?.label ?: if (isHoliday) "周末假期" else null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (showHolidays && isHoliday) FluxHolidayOrange.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onDateClick(date) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(date, style = MaterialTheme.typography.titleMedium)
                    if (showHolidays && holidayLabel != null) {
                        Text(
                            text = holidayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = FluxHolidayOrange
                        )
                    }
                    Text(
                        text = aggregation.weekSummary(showEvents = showEvents),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (aggregation?.hasDiary == true) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FluxDiaryYellow))
                    }
                    if ((aggregation?.pendingTodosCount ?: 0) > 0) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FluxTodoRed))
                    }
                    if (showEvents && aggregation?.eventColors?.isNotEmpty() == true) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(aggregation.eventColors.first())))
                        )
                    }
                }
            }
        }
    }
}
