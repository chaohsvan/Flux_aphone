package com.example.flux.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.HolidayRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.calendar.CalendarAggregatorUseCase
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class CalendarMonth(
    val year: Int,
    val month: Int
) {
    val label: String
        get() = String.format(Locale.CHINA, "%d年 %d月", year, month)

    fun next(): CalendarMonth {
        return if (month == 12) CalendarMonth(year + 1, 1) else copy(month = month + 1)
    }

    fun previous(): CalendarMonth {
        return if (month == 1) CalendarMonth(year - 1, 12) else copy(month = month - 1)
    }

    fun lengthOfMonth(): Int {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOffset(): Int {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun dateString(day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun isWeekend(day: Int): Boolean {
        val dayOfWeek = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    companion object {
        fun current(): CalendarMonth {
            val calendar = Calendar.getInstance()
            return CalendarMonth(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1
            )
        }
    }
}

data class CalendarDateDetails(
    val diary: DiaryEntity? = null,
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val isHoliday: Boolean = false,
    val holidayLabel: String? = null
)

data class HolidayOverrideState(
    val isHoliday: Boolean,
    val label: String?
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    calendarAggregatorUseCase: CalendarAggregatorUseCase,
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository,
    private val holidayRepository: HolidayRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(CalendarMonth.current())
    val currentMonth: StateFlow<CalendarMonth> = _currentMonth.asStateFlow()

    val aggregatedData: StateFlow<Map<String, DailyAggregation>> = calendarAggregatorUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _showDiaries = MutableStateFlow(false)
    val showDiaries = _showDiaries.asStateFlow()

    private val _showTodos = MutableStateFlow(false)
    val showTodos = _showTodos.asStateFlow()

    private val _showHolidays = MutableStateFlow(true)
    val showHolidays = _showHolidays.asStateFlow()

    private val _holidayEditMode = MutableStateFlow(false)
    val holidayEditMode = _holidayEditMode.asStateFlow()

    val holidayOverrides: StateFlow<Map<String, HolidayOverrideState>> = holidayRepository.getHolidayOverrides()
        .map { overrides ->
            overrides.associate { override ->
                override.date to HolidayOverrideState(
                    isHoliday = override.isHoliday == 1,
                    label = override.label
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateDetails: StateFlow<CalendarDateDetails?> = _selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(null)
            } else {
                combine(
                    diaryRepository.getDiaryFlowByDate(date),
                    todoRepository.getTodosByDate(date),
                    eventRepository.getEventsByDate(date),
                    holidayOverrides
                ) { diary, todos, events, overrides ->
                    val defaultHoliday = isWeekend(date)
                    val override = overrides[date]
                    val isHoliday = override?.isHoliday ?: defaultHoliday
                    CalendarDateDetails(
                        diary = diary,
                        todos = todos,
                        events = events,
                        isHoliday = isHoliday,
                        holidayLabel = override?.label ?: if (defaultHoliday) "周末假期" else null
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleShowDiaries() {
        _showDiaries.update { !it }
    }

    fun toggleShowTodos() {
        _showTodos.update { !it }
    }

    fun toggleShowHolidays() {
        _showHolidays.update { !it }
    }

    fun toggleHolidayEditMode() {
        _holidayEditMode.update { !it }
    }

    fun selectDate(date: String?) {
        _selectedDate.value = date
    }

    fun onDateClicked(date: String, defaultIsHoliday: Boolean) {
        if (_holidayEditMode.value) {
            viewModelScope.launch {
                holidayRepository.toggleHolidayOverride(date, defaultIsHoliday)
            }
        } else {
            selectDate(date)
        }
    }

    fun nextMonth() {
        _currentMonth.update { it.next() }
    }

    fun previousMonth() {
        _currentMonth.update { it.previous() }
    }

    fun addEvent(title: String, startAt: String) {
        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val event = CalendarEventEntity(
                id = TimeUtil.generateUuid(),
                title = title,
                description = "",
                startAt = startAt,
                endAt = startAt,
                allDay = 0,
                color = "#4A90E2",
                locationName = null,
                reminderMinutes = null,
                recurrenceRule = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            eventRepository.saveEvent(event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            eventRepository.softDeleteEvent(id)
        }
    }

    private fun isWeekend(date: String): Boolean {
        if (date.length < 10) return false
        val year = date.substring(0, 4).toIntOrNull() ?: return false
        val month = date.substring(5, 7).toIntOrNull() ?: return false
        val day = date.substring(8, 10).toIntOrNull() ?: return false
        val dayOfWeek = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
}
