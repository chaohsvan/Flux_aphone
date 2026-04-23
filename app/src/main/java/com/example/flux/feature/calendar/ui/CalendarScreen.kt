package com.example.flux.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val data by viewModel.aggregatedData.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val showDiaries by viewModel.showDiaries.collectAsState()
    val showTodos by viewModel.showTodos.collectAsState()
    val showHolidays by viewModel.showHolidays.collectAsState()
    val holidayEditMode by viewModel.holidayEditMode.collectAsState()
    val holidayOverrides by viewModel.holidayOverrides.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateDetails by viewModel.selectedDateDetails.collectAsState()
    var showEventInputSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentMonth.label) },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月")
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            CalendarLayerToggles(
                showDiaries = showDiaries,
                showTodos = showTodos,
                showHolidays = showHolidays,
                holidayEditMode = holidayEditMode,
                onToggleDiaries = viewModel::toggleShowDiaries,
                onToggleTodos = viewModel::toggleShowTodos,
                onToggleHolidays = viewModel::toggleShowHolidays,
                onToggleHolidayEditMode = viewModel::toggleHolidayEditMode
            )

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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
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
                        val aggregation = data[dateString]
                        val defaultHoliday = currentMonth.isWeekend(day)
                        val holidayOverride = holidayOverrides[dateString]
                        val isHoliday = holidayOverride?.isHoliday ?: defaultHoliday

                        CalendarGridCell(
                            day = day,
                            aggregation = aggregation,
                            showDiaries = showDiaries,
                            showTodos = showTodos,
                            showHolidays = showHolidays,
                            isHoliday = isHoliday,
                            isHolidayOverride = holidayOverride != null,
                            holidayEditMode = holidayEditMode,
                            onClick = { viewModel.onDateClicked(dateString, defaultHoliday) }
                        )
                    }
                }
            }
        }
    }

    if (selectedDate != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.selectDate(null) }) {
            CalendarDateDetailsSheet(
                selectedDate = selectedDate.orEmpty(),
                details = selectedDateDetails,
                onAddEvent = { showEventInputSheet = true },
                onDeleteEvent = viewModel::deleteEvent
            )
        }
    }

    if (showEventInputSheet && selectedDate != null) {
        EventInputSheet(
            targetDate = selectedDate!!,
            onDismiss = { showEventInputSheet = false },
            onSubmit = { title, startAt ->
                viewModel.addEvent(title, startAt)
                showEventInputSheet = false
            }
        )
    }
}

@Composable
private fun CalendarLayerToggles(
    showDiaries: Boolean,
    showTodos: Boolean,
    showHolidays: Boolean,
    holidayEditMode: Boolean,
    onToggleDiaries: () -> Unit,
    onToggleTodos: () -> Unit,
    onToggleHolidays: () -> Unit,
    onToggleHolidayEditMode: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = showDiaries,
                onClick = onToggleDiaries,
                label = { Text("日记") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FluxDiaryYellow)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = showHolidays,
                onClick = onToggleHolidays,
                label = { Text("假期") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FluxHolidayOrange)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = holidayEditMode,
                onClick = onToggleHolidayEditMode,
                label = { Text("标记假期") }
            )
        }
        item {
            FilterChip(
                selected = showTodos,
                onClick = onToggleTodos,
                label = { Text("待办") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FluxTodoRed)
                    )
                }
            )
        }
    }
}

@Composable
private fun CalendarDateDetailsSheet(
    selectedDate: String,
    details: CalendarDateDetails?,
    onAddEvent: () -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = selectedDate,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (details != null) {
            if (details.holidayLabel != null) {
                Text(
                    text = details.holidayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (details.isHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("事件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddEvent) {
                    Icon(Icons.Default.Add, contentDescription = "添加事件")
                }
            }

            details.events.forEach { event ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "- ${event.title} (${event.startAt.substring(11, 16)})",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteEvent(event.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除事件",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (details.diary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("日记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "- ${details.diary.title.ifBlank { "无标题日记" }}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (details.todos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("待办", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                details.todos.forEach { todo ->
                    Text(
                        text = "- ${todo.title}",
                        textDecoration = if (todo.status == "completed") TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (details.events.isEmpty() && details.diary == null && details.todos.isEmpty()) {
                Text("这一天还没有记录。")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CalendarGridCell(
    day: Int,
    aggregation: DailyAggregation?,
    showDiaries: Boolean,
    showTodos: Boolean,
    showHolidays: Boolean,
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
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(FluxDiaryYellow)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (showTodos && aggregation.pendingTodosCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(FluxTodoRed)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (showTodos && aggregation.completedTodosCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }
                }
            }
        }
    }
}
