package com.example.flux.feature.calendar.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.abs
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.feature.todo.ui.TodoInputSheet
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class CalendarViewMode {
    MONTH,
    DAY,
    WEEK,
    QUARTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToDiary: (String?, String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    focusDateRequest: String? = null,
    onFocusDateHandled: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val data by viewModel.aggregatedData.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val showDiaries by viewModel.showDiaries.collectAsState()
    val showTodos by viewModel.showTodos.collectAsState()
    val showEvents by viewModel.showEvents.collectAsState()
    val showHolidays by viewModel.showHolidays.collectAsState()
    val showTrash by viewModel.showTrash.collectAsState()
    val holidayEditMode by viewModel.holidayEditMode.collectAsState()
    val holidayOverrides by viewModel.holidayOverrides.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateDetails by viewModel.selectedDateDetails.collectAsState()
    val monthHistory by viewModel.monthHistory.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val trashSummary by viewModel.trashSummary.collectAsState()
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    val displayDate = selectedDate ?: currentMonth.dateString(1)
    val toolbarTitle = when (viewMode) {
        CalendarViewMode.MONTH -> currentMonth.label
        CalendarViewMode.DAY -> displayDate
        CalendarViewMode.WEEK -> displayDate.weekRangeLabel()
        CalendarViewMode.QUARTER -> currentMonth.quarterLabel()
    }
    var showEventInputSheet by remember { mutableStateOf(false) }
    var showTodoInputSheet by remember { mutableStateOf(false) }
    var showMonthHistorySheet by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    LaunchedEffect(focusDateRequest) {
        if (!focusDateRequest.isNullOrBlank()) {
            viewModel.focusDate(focusDateRequest)
            viewMode = CalendarViewMode.DAY
            onFocusDateHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(toolbarTitle) },
                actions = {
                    IconButton(onClick = onOpenGlobalSearch) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "\u7edf\u4e00\u641c\u7d22"
                        )
                    }
                    TextButton(onClick = onNavigateToTrash) {
                        Text(if (trashSummary.total > 0) "\u56de\u6536\u7ad9 ${trashSummary.total}" else "\u56de\u6536\u7ad9")
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
            CalendarControlStrip(
                showDiaries = showDiaries,
                showTodos = showTodos,
                showEvents = showEvents,
                showHolidays = showHolidays,
                showTrash = showTrash,
                holidayEditMode = holidayEditMode,
                viewMode = viewMode,
                isExpanded = showLayerPanel || holidayEditMode,
                onToggleExpand = { showLayerPanel = !showLayerPanel },
                onToggleHolidayEditMode = viewModel::toggleHolidayEditMode,
                onShowMonthHistory = { showMonthHistorySheet = true }
            )

            AnimatedVisibility(visible = showLayerPanel || holidayEditMode) {
                CalendarLayerToggles(
                    showDiaries = showDiaries,
                    showTodos = showTodos,
                    showEvents = showEvents,
                    showHolidays = showHolidays,
                    showTrash = showTrash,
                    onToggleDiaries = viewModel::toggleShowDiaries,
                    onToggleTodos = viewModel::toggleShowTodos,
                    onToggleEvents = viewModel::toggleShowEvents,
                    onToggleHolidays = viewModel::toggleShowHolidays,
                    onToggleTrash = viewModel::toggleShowTrash
                )
            }

            CalendarModeChips(
                viewMode = viewMode,
                onViewModeChange = { mode ->
                    viewMode = mode
                    if (mode == CalendarViewMode.DAY && selectedDate == null) {
                        viewModel.selectDate(currentMonth.dateString(1))
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(viewMode, currentMonth, selectedDate) {
                        var dragDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                dragDistance += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                if (abs(dragDistance) < 72f) return@detectHorizontalDragGestures
                                when {
                                    dragDistance < 0 -> {
                                        when (viewMode) {
                                            CalendarViewMode.MONTH -> viewModel.nextMonth()
                                            CalendarViewMode.DAY -> viewModel.shiftSelectedDate(1)
                                            CalendarViewMode.WEEK -> viewModel.shiftSelectedDate(7)
                                            CalendarViewMode.QUARTER -> viewModel.shiftCurrentMonth(3)
                                        }
                                    }

                                    dragDistance > 0 -> {
                                        when (viewMode) {
                                            CalendarViewMode.MONTH -> viewModel.previousMonth()
                                            CalendarViewMode.DAY -> viewModel.shiftSelectedDate(-1)
                                            CalendarViewMode.WEEK -> viewModel.shiftSelectedDate(-7)
                                            CalendarViewMode.QUARTER -> viewModel.shiftCurrentMonth(-3)
                                        }
                                    }
                                }
                                dragDistance = 0f
                            }
                        )
                    }
            ) {
            if (viewMode == CalendarViewMode.MONTH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    listOf(
                        "\u65e5",
                        "\u4e00",
                        "\u4e8c",
                        "\u4e09",
                        "\u56db",
                        "\u4e94",
                        "\u516d"
                    ).forEach { day ->
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
                                onClick = { viewModel.onDateClicked(dateString, baseHoliday) }
                            )
                        }
                    }
                }
            } else if (viewMode == CalendarViewMode.DAY) {
                val date = selectedDate ?: currentMonth.dateString(1)
                CalendarDayTimelineView(
                    selectedDate = date,
                    details = selectedDateDetails,
                    showEvents = showEvents,
                    showTrash = showTrash,
                    onWriteDiary = { diaryId -> onNavigateToDiary(diaryId, date) },
                    onAddTodo = { showTodoInputSheet = true },
                    onAddEvent = {
                        viewModel.selectDate(date)
                        showEventInputSheet = true
                    },
                    onEditEvent = { event ->
                        editingEvent = event
                        showEventInputSheet = true
                    },
                    onDeleteEvent = viewModel::deleteEvent,
                    onRestoreDiary = { diaryId ->
                        viewModel.restoreDiary(diaryId)
                        Toast.makeText(context, "\u5df2\u6062\u590d\u65e5\u8bb0", Toast.LENGTH_SHORT).show()
                    },
                    onRestoreTodo = { todoId ->
                        viewModel.restoreTodo(todoId)
                        Toast.makeText(context, "\u5df2\u6062\u590d\u5f85\u529e", Toast.LENGTH_SHORT).show()
                    },
                    onRestoreEvent = { eventId ->
                        viewModel.restoreEvent(eventId)
                        Toast.makeText(context, "\u5df2\u6062\u590d\u4e8b\u4ef6", Toast.LENGTH_SHORT).show()
                    }
                )
            } else if (viewMode == CalendarViewMode.WEEK) {
                CalendarWeekView(
                    selectedDate = selectedDate ?: currentMonth.dateString(1),
                    aggregatedData = data,
                    showEvents = showEvents,
                    showHolidays = showHolidays,
                    holidayOverrides = holidayOverrides,
                    onDateClick = { date ->
                        viewModel.selectDate(date)
                        viewMode = CalendarViewMode.DAY
                    }
                )
            } else {
                CalendarQuarterView(
                    currentMonth = currentMonth,
                    aggregatedData = data,
                    showEvents = showEvents,
                    showHolidays = showHolidays,
                    holidayOverrides = holidayOverrides,
                    onDateClick = { date ->
                        viewModel.selectDate(date)
                        viewMode = CalendarViewMode.DAY
                    }
                )
            }
            }
        }
    }

    if (selectedDate != null && viewMode == CalendarViewMode.MONTH) {
        ModalBottomSheet(onDismissRequest = { viewModel.selectDate(null) }) {
            CalendarDateDetailsSheet(
                selectedDate = selectedDate.orEmpty(),
                details = selectedDateDetails,
                showEvents = showEvents,
                showTrash = showTrash,
                onWriteDiary = { diaryId ->
                    onNavigateToDiary(diaryId, selectedDate.orEmpty())
                    viewModel.selectDate(null)
                },
                onAddTodo = { showTodoInputSheet = true },
                onAddEvent = { showEventInputSheet = true },
                onEditEvent = { event ->
                    editingEvent = event
                    showEventInputSheet = true
                },
                onDeleteEvent = viewModel::deleteEvent,
                onRestoreDiary = { diaryId ->
                    viewModel.restoreDiary(diaryId)
                    Toast.makeText(context, "\u5df2\u6062\u590d\u65e5\u8bb0", Toast.LENGTH_SHORT).show()
                },
                onRestoreTodo = { todoId ->
                    viewModel.restoreTodo(todoId)
                    Toast.makeText(context, "\u5df2\u6062\u590d\u5f85\u529e", Toast.LENGTH_SHORT).show()
                },
                onRestoreEvent = { eventId ->
                    viewModel.restoreEvent(eventId)
                    Toast.makeText(context, "\u5df2\u6062\u590d\u4e8b\u4ef6", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showEventInputSheet && selectedDate != null) {
        EventInputSheet(
            targetDate = selectedDate!!,
            event = editingEvent,
            onDismiss = {
                editingEvent = null
                showEventInputSheet = false
            },
            onSubmit = { input ->
                val currentEvent = editingEvent
                if (currentEvent == null) {
                    viewModel.addEvent(input)
                    Toast.makeText(context, "\u5df2\u521b\u5efa\u4e8b\u4ef6", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateEvent(currentEvent, input)
                    Toast.makeText(context, "\u5df2\u66f4\u65b0\u4e8b\u4ef6", Toast.LENGTH_SHORT).show()
                }
                editingEvent = null
                showEventInputSheet = false
            }
        )
    }

    if (showTodoInputSheet && selectedDate != null) {
        TodoInputSheet(
            projects = projects,
            initialDueAt = selectedDate!!,
            onDismiss = { showTodoInputSheet = false },
            onCreateProject = viewModel::addProject,
            onSubmit = { title, desc, priority, projectId, dueAt ->
                viewModel.addTodo(title, desc, priority, projectId, dueAt)
                Toast.makeText(context, "\u5df2\u521b\u5efa\u5f85\u529e", Toast.LENGTH_SHORT).show()
                showTodoInputSheet = false
            }
        )
    }

    if (showMonthHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showMonthHistorySheet = false }) {
            CalendarMonthHistorySheet(
                selectedDate = selectedDate ?: currentMonth.dateString(1),
                history = monthHistory
            )
        }
    }
}

@Composable
private fun CalendarControlStrip(
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
        CalendarViewMode.MONTH -> "\u5de6\u53f3\u6ed1\u52a8\u5207\u6362\u6708\u4efd"
        CalendarViewMode.DAY -> "\u5de6\u53f3\u6ed1\u52a8\u5207\u6362\u65e5\u671f"
        CalendarViewMode.WEEK -> "\u5de6\u53f3\u6ed1\u52a8\u5207\u6362\u5468"
        CalendarViewMode.QUARTER -> "\u5de6\u53f3\u6ed1\u52a8\u5207\u6362\u5b63\u5ea6"
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
            TextButton(
                onClick = onToggleExpand,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isExpanded) {
                        "\u6536\u8d77\u56fe\u5c42 | $activeLayerCount"
                    } else {
                        "\u56fe\u5c42 | $activeLayerCount"
                    }
                )
            }
            TextButton(
                onClick = onToggleHolidayEditMode,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (holidayEditMode) {
                        "\u5b8c\u6210\u65e5\u671f\u6807\u8bb0"
                    } else {
                        "\u65e5\u671f\u6807\u8bb0"
                    }
                )
            }
            TextButton(
                onClick = onShowMonthHistory,
                modifier = Modifier.weight(1f)
            ) {
                Text("\u540c\u6708\u8bb0\u5f55")
            }
        }

        Text(
            text = if (holidayEditMode) {
                "\u6807\u8bb0\u6a21\u5f0f\u5df2\u5f00\u542f\uff0c\u70b9\u51fb\u65e5\u671f\u5373\u53ef\u5728\u4f11 / \u73ed\u4e4b\u95f4\u5207\u6362"
            } else {
                swipeHint
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (holidayEditMode) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun CalendarModeChips(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalendarModeChip(
            label = "\u6708",
            selected = viewMode == CalendarViewMode.MONTH,
            onClick = { onViewModeChange(CalendarViewMode.MONTH) },
            modifier = Modifier.weight(1f)
        )
        CalendarModeChip(
            label = "\u65e5",
            selected = viewMode == CalendarViewMode.DAY,
            onClick = { onViewModeChange(CalendarViewMode.DAY) },
            modifier = Modifier.weight(1f)
        )
        CalendarModeChip(
            label = "\u5468",
            selected = viewMode == CalendarViewMode.WEEK,
            onClick = { onViewModeChange(CalendarViewMode.WEEK) },
            modifier = Modifier.weight(1f)
        )
        CalendarModeChip(
            label = "\u5b63",
            selected = viewMode == CalendarViewMode.QUARTER,
            onClick = { onViewModeChange(CalendarViewMode.QUARTER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CalendarModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
private fun CalendarQuarterView(
    currentMonth: CalendarMonth,
    aggregatedData: Map<String, DailyAggregation>,
    showEvents: Boolean,
    showHolidays: Boolean,
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
                    listOf(
                        "\u65e5",
                        "\u4e00",
                        "\u4e8c",
                        "\u4e09",
                        "\u56db",
                        "\u4e94",
                        "\u516d"
                    ).forEach { day ->
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
                val startOffset = month.firstDayOffset()
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
                                    holidayLabel = holidayState?.label ?: if (isHoliday) "\u4f11" else null,
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
private fun QuarterDayCell(
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
            if (aggregation?.hasDiary == true) add("\u8bb0")
            val pendingCount = aggregation?.pendingTodosCount ?: 0
            if (pendingCount > 0) add("\u5f85$pendingCount")
            val eventCount = aggregation?.eventColors?.size ?: 0
            if (showEvents && eventCount > 0) add("\u4e8b$eventCount")
            val deletedCount = aggregation?.deletedCount ?: 0
            if (deletedCount > 0) add("\u5220$deletedCount")
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
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(aggregation.eventColors.first()))))
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
private fun CalendarWeekView(
    selectedDate: String,
    aggregatedData: Map<String, DailyAggregation>,
    showEvents: Boolean,
    showHolidays: Boolean,
    holidayOverrides: Map<String, HolidayOverrideState>,
    onDateClick: (String) -> Unit
) {
    val weekDates = remember(selectedDate) { selectedDate.weekDates() }
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
                        if (showHolidays && isHoliday) {
                            FluxHolidayOrange.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
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
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(aggregation.eventColors.first()))))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayTimelineView(
    selectedDate: String,
    details: CalendarDateDetails?,
    showEvents: Boolean,
    showTrash: Boolean,
    onWriteDiary: (String?) -> Unit,
    onAddTodo: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onRestoreDiary: (String) -> Unit,
    onRestoreTodo: (String) -> Unit,
    onRestoreEvent: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(selectedDate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (details?.holidayLabel != null) {
                    Text(
                        text = details.holidayLabel,
                        color = if (details.isHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onWriteDiary(details?.diary?.id) }, modifier = Modifier.weight(1f)) {
                        Text(if (details?.diary == null) "\u5199\u65e5\u8bb0" else "\u6253\u5f00\u65e5\u8bb0")
                    }
                    Button(onClick = onAddTodo, modifier = Modifier.weight(1f)) {
                        Text("\u52a0\u5f85\u529e")
                    }
                    Button(onClick = onAddEvent, modifier = Modifier.weight(1f)) {
                        Text("\u52a0\u4e8b\u4ef6")
                    }
                }
            }
        }

        if (details == null) {
            item {
                Text(
                    text = "\u6b63\u5728\u52a0\u8f7d\u8fd9\u4e00\u5929\u7684\u5b89\u6392...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            return@LazyColumn
        }

        val allDayEvents = if (showEvents) details.events.filter { it.allDay == 1 } else emptyList()
        if (details.diary != null || allDayEvents.isNotEmpty()) {
            item {
                Text("\u5168\u5929", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            details.diary?.let { diary ->
                item {
                    TimelineRow(
                        time = "\u65e5\u8bb0",
                        title = diary.title.ifBlank { "\u65e0\u6807\u9898\u65e5\u8bb0" },
                        subtitle = diary.contentMd.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                        color = FluxDiaryYellow,
                        onClick = { onWriteDiary(diary.id) }
                    )
                }
            }
            items(allDayEvents, key = { it.id }) { event ->
                TimelineRow(
                        time = "\u5168\u5929",
                    title = event.title,
                    subtitle = event.eventSubtitle(),
                    color = event.safeColor(),
                    onClick = { onEditEvent(event) },
                    trailing = {
                        IconButton(onClick = { onDeleteEvent(event.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "\u5220\u9664\u4e8b\u4ef6",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        }

        val deletedCount = details.deletedDiaries.size + details.deletedTodos.size + details.deletedEvents.size
        if (showTrash && deletedCount > 0) {
            item {
                Text("\u56de\u6536\u7ad9", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            details.deletedDiaries.forEach { diary ->
                item(key = "deleted_diary:${diary.id}") {
                    TimelineRow(
                        time = "\u5220",
                        title = diary.title.ifBlank { "\u65e0\u6807\u9898\u65e5\u8bb0" },
                        subtitle = diary.entryDate,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreDiary(diary.id) }) {
                                Text("鎭㈠")
                            }
                        }
                    )
                }
            }
            details.deletedTodos.forEach { todo ->
                item(key = "deleted_todo:${todo.id}") {
                    TimelineRow(
                        time = "\u5220",
                        title = todo.title,
                        subtitle = todo.dueAt ?: todo.createdAt,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreTodo(todo.id) }) {
                                Text("鎭㈠")
                            }
                        }
                    )
                }
            }
            details.deletedEvents.forEach { event ->
                item(key = "deleted_event:${event.id}") {
                    TimelineRow(
                        time = "\u5220",
                        title = event.title,
                        subtitle = event.startAt,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreEvent(event.id) }) {
                                Text("鎭㈠")
                            }
                        }
                    )
                }
            }
        }

        val timedEvents = if (showEvents) details.events.filterNot { it.allDay == 1 } else emptyList()
        val timelineItems = buildList {
            timedEvents.forEach { event ->
                add(CalendarTimelineEntry.Event(event.timelineTime(), event))
            }
            details.todos.forEach { todo ->
                add(CalendarTimelineEntry.Todo(todo.timelineTime(), todo))
            }
        }.sortedWith(compareBy<CalendarTimelineEntry> { it.sortKey }.thenBy { it.title })

        item {
            Text("\u65f6\u95f4\u8f74", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (timelineItems.isEmpty()) {
            item {
                Text(
                    text = "\u8fd9\u4e00\u5929\u8fd8\u6ca1\u6709\u5b9a\u65f6\u5b89\u6392\u3002",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            items(timelineItems, key = { it.key }) { entry ->
                when (entry) {
                    is CalendarTimelineEntry.Event -> {
                        TimelineRow(
                            time = entry.time,
                            title = entry.event.title,
                            subtitle = entry.event.eventSubtitle(),
                            color = entry.event.safeColor(),
                            onClick = { onEditEvent(entry.event) },
                            trailing = {
                                IconButton(onClick = { onDeleteEvent(entry.event.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "\u5220\u9664\u4e8b\u4ef6",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                    is CalendarTimelineEntry.Todo -> {
                        TimelineRow(
                            time = entry.time,
                            title = entry.todo.title,
                            subtitle = entry.todo.todoSubtitle(),
                            color = if (entry.todo.status == "completed") Color.Gray else FluxTodoRed,
                            textDecoration = if (entry.todo.status == "completed") TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    time: String,
    title: String,
    subtitle: String,
    color: Color,
    textDecoration: TextDecoration? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            modifier = Modifier.width(58.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textDecoration = textDecoration
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

private sealed class CalendarTimelineEntry(
    val key: String,
    val time: String,
    val sortKey: String,
    val title: String
) {
    class Event(time: String, val event: CalendarEventEntity) :
        CalendarTimelineEntry("event:${event.id}", time, time.sortableTime(), event.title)

    class Todo(time: String, val todo: TodoEntity) :
        CalendarTimelineEntry("todo:${todo.id}", time, time.sortableTime(), todo.title)
}

@Composable
private fun CalendarMonthHistorySheet(
    selectedDate: String,
    history: CalendarMonthHistory
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val month = selectedDate.takeIf { it.length >= 7 }?.substring(5, 7).orEmpty()
        Text("\u90a3\u5e74\u8fd9\u6708 | $month \u6708", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        val totalCount = history.diaries.size + history.todos.size + history.events.size
        if (totalCount == 0) {
            Text("\u6ca1\u6709\u627e\u5230\u5386\u53f2\u540c\u6708\u8bb0\u5f55\u3002", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = "${history.diaries.size} \u7bc7\u65e5\u8bb0 | ${history.todos.size} \u4e2a\u5f85\u529e | ${history.events.size} \u4e2a\u4e8b\u4ef6",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            history.diaries.take(5).forEach { diary ->
                Text("\u65e5\u8bb0 ${diary.entryDate}  ${diary.title.ifBlank { diary.contentMd.take(20) }}")
            }
            history.todos.take(5).forEach { todo ->
                Text("\u5f85\u529e ${todo.dueAt?.take(10).orEmpty()}  ${todo.title}")
            }
            history.events.take(5).forEach { event ->
                Text("\u4e8b\u4ef6 ${event.startAt.take(10)}  ${event.title}")
            }

            val hiddenCount = totalCount - history.diaries.take(5).size - history.todos.take(5).size - history.events.take(5).size
            if (hiddenCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\u8fd8\u6709 $hiddenCount \u6761\u672a\u663e\u793a\u3002", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CalendarLayerToggles(
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = showDiaries,
                    onClick = onToggleDiaries,
                    label = { Text("\u65e5\u8bb0") },
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
                    label = { Text("\u5047\u671f") },
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
                    selected = showTodos,
                    onClick = onToggleTodos,
                    label = { Text("\u5f85\u529e") },
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
            item {
                FilterChip(
                    selected = showEvents,
                    onClick = onToggleEvents,
                    label = { Text("\u4e8b\u4ef6") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TimelineFallbackColor)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = showTrash,
                    onClick = onToggleTrash,
                    label = { Text("\u56de\u6536\u7ad9") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarDateDetailsSheet(
    selectedDate: String,
    details: CalendarDateDetails?,
    showEvents: Boolean,
    showTrash: Boolean,
    onWriteDiary: (String?) -> Unit,
    onAddTodo: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onRestoreDiary: (String) -> Unit,
    onRestoreTodo: (String) -> Unit,
    onRestoreEvent: (String) -> Unit
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onWriteDiary(details.diary?.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (details.diary == null) "\u5199\u65e5\u8bb0" else "\u6253\u5f00\u65e5\u8bb0")
                }
                Button(
                    onClick = onAddTodo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("\u52a0\u5f85\u529e")
                }
                Button(
                    onClick = onAddEvent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("\u52a0\u4e8b\u4ef6")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u4e8b\u4ef6", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            details.events.forEach { event ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = event.label(),
                        modifier = Modifier.weight(1f)
                            .clickable { onEditEvent(event) }
                    )
                    IconButton(onClick = { onDeleteEvent(event.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "\u5220\u9664\u4e8b\u4ef6",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (details.diary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\u65e5\u8bb0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "- ${details.diary.title.ifBlank { "\u65e0\u6807\u9898\u65e5\u8bb0" }}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (details.todos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\u5f85\u529e", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                details.todos.forEach { todo ->
                    Text(
                        text = "- ${todo.title}",
                        textDecoration = if (todo.status == "completed") TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            val deletedCount = details.deletedDiaries.size + details.deletedTodos.size + details.deletedEvents.size
            if (showTrash && deletedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\u56de\u6536\u7ad9", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                details.deletedDiaries.forEach { diary ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u5220 | \u65e5\u8bb0 ${diary.title.ifBlank { diary.entryDate }}", color = Color.Gray)
                        TextButton(onClick = { onRestoreDiary(diary.id) }) {
                            Text("\u6062\u590d")
                        }
                    }
                }
                details.deletedTodos.forEach { todo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u5220 | \u5f85\u529e ${todo.title}", color = Color.Gray)
                        TextButton(onClick = { onRestoreTodo(todo.id) }) {
                            Text("\u6062\u590d")
                        }
                    }
                }
                details.deletedEvents.forEach { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u5220 | \u4e8b\u4ef6 ${event.title}", color = Color.Gray)
                        TextButton(onClick = { onRestoreEvent(event.id) }) {
                            Text("\u6062\u590d")
                        }
                    }
                }
            }

            if (details.events.isEmpty() && details.diary == null && details.todos.isEmpty() && (!showTrash || deletedCount == 0)) {
                Text("\u8fd9\u4e00\u5929\u8fd8\u6ca1\u6709\u8bb0\u5f55\u3002")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun CalendarEventEntity.label(): String {
    val time = if (allDay == 1) {
        "鍏ㄥぉ"
    } else if (startAt.length >= 16) {
        startAt.substring(11, 16)
    } else {
        ""
    }
    val place = locationName?.takeIf { it.isNotBlank() }?.let { " @ $it" }.orEmpty()
    return "- $title${if (time.isBlank()) "" else " ($time)"}$place"
}

private fun CalendarEventEntity.timelineTime(): String {
    return if (startAt.length >= 16) startAt.substring(11, 16) else "--:--"
}

private fun CalendarEventEntity.eventSubtitle(): String {
    val parts = buildList {
        if (allDay == 1) {
            add("鍏ㄥぉ")
        } else {
            val start = startAt.takeIf { it.length >= 16 }?.substring(11, 16)
            val end = endAt.takeIf { it.length >= 16 }?.substring(11, 16)
            if (start != null && end != null) add("$start-$end")
        }
        locationName?.takeIf { it.isNotBlank() }?.let { add(it) }
        reminderMinutes?.let { add("\u63d0\u524d $it \u5206\u949f\u63d0\u9192") }
        recurrenceRule?.takeIf { it.isNotBlank() }?.let { add(it.recurrenceLabel()) }
    }
    return parts.joinToString(" / ")
}

private fun CalendarEventEntity.safeColor(): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(color ?: "#4A90E2"))
    }.getOrDefault(TimelineFallbackColor)
}

private fun TodoEntity.timelineTime(): String {
    val raw = dueAt ?: startAt
    return raw?.takeIf { it.length >= 16 }?.substring(11, 16) ?: "--:--"
}

private fun TodoEntity.todoSubtitle(): String {
    val parts = buildList {
        startAt?.takeIf { it.isNotBlank() }?.let { add("\u5f00\u59cb $it") }
        dueAt?.takeIf { it.isNotBlank() }?.let { add("\u622a\u6b62 $it") }
        reminderMinutes?.let { add("\u63d0\u524d $it \u5206\u949f\u63d0\u9192") }
        recurrence.takeIf { it != "none" }?.let { add(it.recurrenceLabel()) }
        if (status == "completed") add("\u5df2\u5b8c\u6210")
    }
    return parts.joinToString(" / ")
}

private fun String.sortableTime(): String {
    return if (Regex("\\d{2}:\\d{2}").matches(this)) this else "99:99"
}

private fun String.recurrenceLabel(): String {
    return RecurrenceUtil.label(this)
}

private val TimelineFallbackColor = Color(0xFF4A90E2)

private fun String.weekDates(): List<String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val baseDate = runCatching { formatter.parse(this) }.getOrNull() ?: return listOf(this)
    val calendar = Calendar.getInstance().apply {
        time = baseDate
        firstDayOfWeek = Calendar.SUNDAY
    }
    val dayOffset = calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek
    calendar.add(Calendar.DAY_OF_MONTH, -dayOffset)
    return List(7) {
        formatter.format(calendar.time).also {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

private fun String.weekRangeLabel(): String {
    val dates = weekDates()
    val start = dates.firstOrNull().orEmpty()
    val end = dates.lastOrNull().orEmpty()
    return if (start.isBlank() || end.isBlank()) this else "$start | $end"
}

private fun CalendarMonth.quarterLabel(): String {
    val firstQuarterMonth = ((month - 1) / 3) * 3 + 1
    val lastQuarterMonth = firstQuarterMonth + 2
    return "${year}\u5e74${firstQuarterMonth}\u6708-${lastQuarterMonth}\u6708"
}

private fun String.isWeekendDate(): Boolean {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = runCatching { formatter.parse(this) }.getOrNull() ?: return false
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
}

private fun CalendarMonth.quarterMonths(): List<CalendarMonth> {
    val firstQuarterMonth = ((month - 1) / 3) * 3 + 1
    return List(3) { offset -> CalendarMonth(year, firstQuarterMonth + offset) }
}

private fun DailyAggregation?.weekSummary(showEvents: Boolean): String {
    if (this == null) return "\u65e0\u5b89\u6392"
    val parts = buildList {
        if (showEvents && eventColors.isNotEmpty()) add("${eventColors.size} \u4e2a\u4e8b\u4ef6")
        if (pendingTodosCount > 0) add("$pendingTodosCount \u4e2a\u5f85\u529e")
        if (completedTodosCount > 0) add("$completedTodosCount 涓凡瀹屾垚")
        if (hasDiary) add("\u6709\u65e5\u8bb0")
        if (deletedCount > 0) add("$deletedCount \u6761\u5df2\u5220")
    }
    return parts.ifEmpty { listOf("\u65e0\u5b89\u6392") }.joinToString(" | ")
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
                    text = if (isHolidayOverride) "\u4f11" else "\u5047",
                    style = MaterialTheme.typography.labelSmall,
                    color = FluxHolidayOrange,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (renderManualWorkday) {
                Text(
                    text = "\u73ed",
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
                            text = "\u5220",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
