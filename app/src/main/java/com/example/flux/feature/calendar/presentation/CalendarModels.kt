package com.example.flux.feature.calendar.presentation

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.domain.trash.TrashSummary
import java.util.Calendar
import java.util.Locale

data class CalendarMonth(
    val year: Int,
    val month: Int
) {
    val label: String
        get() = String.format(Locale.CHINA, "%d年%d月", year, month)

    fun next(): CalendarMonth {
        return if (month == 12) CalendarMonth(year + 1, 1) else copy(month = month + 1)
    }

    fun previous(): CalendarMonth {
        return if (month == 1) CalendarMonth(year - 1, 12) else copy(month = month - 1)
    }

    fun shift(offset: Int): CalendarMonth {
        if (offset == 0) return this
        var nextYear = year
        var nextMonth = month + offset
        while (nextMonth > 12) {
            nextMonth -= 12
            nextYear += 1
        }
        while (nextMonth < 1) {
            nextMonth += 12
            nextYear -= 1
        }
        return CalendarMonth(nextYear, nextMonth)
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
    val holidayLabel: String? = null,
    val deletedDiaries: List<DiaryEntity> = emptyList(),
    val deletedTodos: List<TodoEntity> = emptyList(),
    val deletedEvents: List<CalendarEventEntity> = emptyList()
)

data class CalendarMonthHistory(
    val diaries: List<DiaryEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList()
)

data class HolidayOverrideState(
    val isHoliday: Boolean,
    val label: String?,
    val defaultIsHoliday: Boolean,
    val isUserOverride: Boolean
)

enum class CalendarSearchResultType {
    DIARY,
    TODO,
    EVENT
}

data class CalendarSearchResult(
    val id: String,
    val type: CalendarSearchResultType,
    val title: String,
    val subtitle: String,
    val date: String
)

data class CalendarContentState(
    val aggregatedData: Map<String, DailyAggregation> = emptyMap(),
    val currentMonth: CalendarMonth = CalendarMonth.current(),
    val projects: List<TodoProjectEntity> = emptyList(),
    val holidayOverrides: Map<String, HolidayOverrideState> = emptyMap(),
    val selectedDateDetails: CalendarDateDetails? = null,
    val monthHistory: CalendarMonthHistory = CalendarMonthHistory(),
    val trashSummary: TrashSummary = TrashSummary()
)

data class CalendarDisplayState(
    val showDiaries: Boolean = false,
    val showTodos: Boolean = false,
    val showEvents: Boolean = true,
    val showHolidays: Boolean = true,
    val showTrash: Boolean = false,
    val holidayEditMode: Boolean = false,
    val selectedDate: String? = null
)

data class CalendarUiState(
    val aggregatedData: Map<String, DailyAggregation> = emptyMap(),
    val currentMonth: CalendarMonth = CalendarMonth.current(),
    val projects: List<TodoProjectEntity> = emptyList(),
    val holidayOverrides: Map<String, HolidayOverrideState> = emptyMap(),
    val selectedDate: String? = null,
    val selectedDateDetails: CalendarDateDetails? = null,
    val monthHistory: CalendarMonthHistory = CalendarMonthHistory(),
    val trashSummary: TrashSummary = TrashSummary(),
    val showDiaries: Boolean = false,
    val showTodos: Boolean = false,
    val showEvents: Boolean = true,
    val showHolidays: Boolean = true,
    val showTrash: Boolean = false,
    val holidayEditMode: Boolean = false
)

internal data class CalendarPrimaryContentState(
    val aggregatedData: Map<String, DailyAggregation> = emptyMap(),
    val currentMonth: CalendarMonth = CalendarMonth.current(),
    val projects: List<TodoProjectEntity> = emptyList(),
    val holidayOverrides: Map<String, HolidayOverrideState> = emptyMap()
)

enum class CalendarViewMode {
    MONTH,
    DAY,
    WEEK,
    QUARTER
}
