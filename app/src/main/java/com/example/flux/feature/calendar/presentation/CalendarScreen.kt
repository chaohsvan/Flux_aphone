package com.example.flux.feature.calendar.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.feature.calendar.presentation.component.CalendarControlStrip
import com.example.flux.feature.calendar.presentation.component.CalendarDateDetailsSheet
import com.example.flux.feature.calendar.presentation.component.CalendarDayTimelineView
import com.example.flux.feature.calendar.presentation.component.CalendarLayerToggles
import com.example.flux.feature.calendar.presentation.component.CalendarModeChips
import com.example.flux.feature.calendar.presentation.component.CalendarMonthHistorySheet
import com.example.flux.feature.calendar.presentation.component.CalendarMonthView
import com.example.flux.feature.calendar.presentation.component.CalendarQuarterView
import com.example.flux.feature.calendar.presentation.component.CalendarWeekView
import com.example.flux.feature.calendar.presentation.component.EventInputSheet
import com.example.flux.feature.calendar.presentation.component.quarterLabel
import com.example.flux.feature.calendar.presentation.component.weekRangeLabel
import com.example.flux.feature.todo.presentation.component.TodoInputSheet
import kotlin.math.abs

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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var showEventInputSheet by remember { mutableStateOf(false) }
    var showTodoInputSheet by remember { mutableStateOf(false) }
    var showMonthHistorySheet by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val displayDate = uiState.selectedDate ?: uiState.currentMonth.dateString(1)
    val toolbarTitle = when (viewMode) {
        CalendarViewMode.MONTH -> uiState.currentMonth.label
        CalendarViewMode.DAY -> displayDate
        CalendarViewMode.WEEK -> displayDate.weekRangeLabel()
        CalendarViewMode.QUARTER -> uiState.currentMonth.quarterLabel()
    }

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
                        Icon(Icons.Default.Search, contentDescription = "统一搜索")
                    }
                    TextButton(onClick = onNavigateToTrash) {
                        Text(if (uiState.trashSummary.total > 0) "回收站 ${uiState.trashSummary.total}" else "回收站")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            CalendarControlStrip(
                showDiaries = uiState.showDiaries,
                showTodos = uiState.showTodos,
                showEvents = uiState.showEvents,
                showHolidays = uiState.showHolidays,
                showTrash = uiState.showTrash,
                holidayEditMode = uiState.holidayEditMode,
                viewMode = viewMode,
                isExpanded = showLayerPanel || uiState.holidayEditMode,
                onToggleExpand = { showLayerPanel = !showLayerPanel },
                onToggleHolidayEditMode = viewModel::toggleHolidayEditMode,
                onShowMonthHistory = { showMonthHistorySheet = true }
            )

            AnimatedVisibility(visible = showLayerPanel || uiState.holidayEditMode) {
                CalendarLayerToggles(
                    showDiaries = uiState.showDiaries,
                    showTodos = uiState.showTodos,
                    showEvents = uiState.showEvents,
                    showHolidays = uiState.showHolidays,
                    showTrash = uiState.showTrash,
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
                    if (mode == CalendarViewMode.DAY && uiState.selectedDate == null) {
                        viewModel.selectDate(uiState.currentMonth.dateString(1))
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(viewMode, uiState.currentMonth, uiState.selectedDate) {
                        var dragDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                dragDistance += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                if (abs(dragDistance) < 72f) return@detectHorizontalDragGestures
                                when {
                                    dragDistance < 0 -> when (viewMode) {
                                        CalendarViewMode.MONTH -> viewModel.nextMonth()
                                        CalendarViewMode.DAY -> viewModel.shiftSelectedDate(1)
                                        CalendarViewMode.WEEK -> viewModel.shiftSelectedDate(7)
                                        CalendarViewMode.QUARTER -> viewModel.shiftCurrentMonth(3)
                                    }

                                    dragDistance > 0 -> when (viewMode) {
                                        CalendarViewMode.MONTH -> viewModel.previousMonth()
                                        CalendarViewMode.DAY -> viewModel.shiftSelectedDate(-1)
                                        CalendarViewMode.WEEK -> viewModel.shiftSelectedDate(-7)
                                        CalendarViewMode.QUARTER -> viewModel.shiftCurrentMonth(-3)
                                    }
                                }
                                dragDistance = 0f
                            }
                        )
                    }
            ) {
                when (viewMode) {
                    CalendarViewMode.MONTH -> CalendarMonthView(
                        currentMonth = uiState.currentMonth,
                        aggregatedData = uiState.aggregatedData,
                        showDiaries = uiState.showDiaries,
                        showTodos = uiState.showTodos,
                        showEvents = uiState.showEvents,
                        showHolidays = uiState.showHolidays,
                        showTrash = uiState.showTrash,
                        holidayEditMode = uiState.holidayEditMode,
                        holidayOverrides = uiState.holidayOverrides,
                        onDateClick = viewModel::onDateClicked
                    )

                    CalendarViewMode.DAY -> {
                        val date = uiState.selectedDate ?: uiState.currentMonth.dateString(1)
                        CalendarDayTimelineView(
                            selectedDate = date,
                            details = uiState.selectedDateDetails,
                            showEvents = uiState.showEvents,
                            showTrash = uiState.showTrash,
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
                                Toast.makeText(context, "已恢复日记", Toast.LENGTH_SHORT).show()
                            },
                            onRestoreTodo = { todoId ->
                                viewModel.restoreTodo(todoId)
                                Toast.makeText(context, "已恢复待办", Toast.LENGTH_SHORT).show()
                            },
                            onRestoreEvent = { eventId ->
                                viewModel.restoreEvent(eventId)
                                Toast.makeText(context, "已恢复事件", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    CalendarViewMode.WEEK -> CalendarWeekView(
                        selectedDate = uiState.selectedDate ?: uiState.currentMonth.dateString(1),
                        aggregatedData = uiState.aggregatedData,
                        showEvents = uiState.showEvents,
                        showHolidays = uiState.showHolidays,
                        holidayOverrides = uiState.holidayOverrides,
                        onDateClick = { date ->
                            viewModel.selectDate(date)
                            viewMode = CalendarViewMode.DAY
                        }
                    )

                    CalendarViewMode.QUARTER -> CalendarQuarterView(
                        currentMonth = uiState.currentMonth,
                        aggregatedData = uiState.aggregatedData,
                        showEvents = uiState.showEvents,
                        showHolidays = uiState.showHolidays,
                        holidayOverrides = uiState.holidayOverrides,
                        onDateClick = { date ->
                            viewModel.selectDate(date)
                            viewMode = CalendarViewMode.DAY
                        }
                    )
                }
            }
        }
    }

    if (uiState.selectedDate != null && viewMode == CalendarViewMode.MONTH) {
        ModalBottomSheet(onDismissRequest = { viewModel.selectDate(null) }) {
            CalendarDateDetailsSheet(
                selectedDate = uiState.selectedDate.orEmpty(),
                details = uiState.selectedDateDetails,
                showTrash = uiState.showTrash,
                onWriteDiary = { diaryId ->
                    onNavigateToDiary(diaryId, uiState.selectedDate.orEmpty())
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
                    Toast.makeText(context, "已恢复日记", Toast.LENGTH_SHORT).show()
                },
                onRestoreTodo = { todoId ->
                    viewModel.restoreTodo(todoId)
                    Toast.makeText(context, "已恢复待办", Toast.LENGTH_SHORT).show()
                },
                onRestoreEvent = { eventId ->
                    viewModel.restoreEvent(eventId)
                    Toast.makeText(context, "已恢复事件", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showEventInputSheet && uiState.selectedDate != null) {
        EventInputSheet(
            targetDate = uiState.selectedDate!!,
            event = editingEvent,
            onDismiss = {
                editingEvent = null
                showEventInputSheet = false
            },
            onSubmit = { input ->
                val currentEvent = editingEvent
                if (currentEvent == null) {
                    viewModel.addEvent(input)
                    Toast.makeText(context, "已创建事件", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateEvent(currentEvent, input)
                    Toast.makeText(context, "已更新事件", Toast.LENGTH_SHORT).show()
                }
                editingEvent = null
                showEventInputSheet = false
            }
        )
    }

    if (showTodoInputSheet && uiState.selectedDate != null) {
        TodoInputSheet(
            projects = uiState.projects,
            initialDueAt = uiState.selectedDate!!,
            onDismiss = { showTodoInputSheet = false },
            onCreateProject = viewModel::addProject,
            onSubmit = { title, desc, priority, projectId, dueAt ->
                viewModel.addTodo(title, desc, priority, projectId, dueAt)
                Toast.makeText(context, "已创建待办", Toast.LENGTH_SHORT).show()
                showTodoInputSheet = false
            }
        )
    }

    if (showMonthHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showMonthHistorySheet = false }) {
            CalendarMonthHistorySheet(
                selectedDate = uiState.selectedDate ?: uiState.currentMonth.dateString(1),
                history = uiState.monthHistory
            )
        }
    }
}
