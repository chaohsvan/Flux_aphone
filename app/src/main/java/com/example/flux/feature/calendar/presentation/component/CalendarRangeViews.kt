package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.feature.calendar.presentation.HolidayOverrideState
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed

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
            val holidayLabel = holidayState?.label ?: if (isHoliday) "\u5468\u672b\u5047\u671f" else null
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Column(
                    modifier = Modifier.width(38.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (aggregation?.hasDiary == true) {
                        WeekContentBar(color = FluxDiaryYellow)
                    }
                    if ((aggregation?.pendingTodosCount ?: 0) > 0) {
                        WeekContentBar(color = FluxTodoRed)
                    }
                    if (showEvents && aggregation?.eventColors?.isNotEmpty() == true) {
                        WeekContentBar(color = Color(android.graphics.Color.parseColor(aggregation.eventColors.first())))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekContentBar(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(color, shape = MaterialTheme.shapes.extraSmall)
    )
}
