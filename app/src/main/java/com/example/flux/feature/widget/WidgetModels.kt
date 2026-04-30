package com.example.flux.feature.widget

import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TodoWidgetState(
    val today: String,
    val pendingCount: Int,
    val overdueCount: Int,
    val highPriorityCount: Int,
    val visibleTodos: List<TodoEntity>
)

data class MonthWidgetState(
    val title: String,
    val cells: List<MonthWidgetDayCell>
)

data class MonthWidgetDayCell(
    val day: Int?,
    val date: String?,
    val isToday: Boolean,
    val aggregation: DailyAggregation?
)

fun List<TodoEntity>.toTodoWidgetState(): TodoWidgetState {
    val today = TimeUtil.getCurrentDate()
    val pendingTodos = filter { it.status != "completed" }
    return TodoWidgetState(
        today = today,
        pendingCount = pendingTodos.size,
        overdueCount = pendingTodos.count { todo ->
            val dueDate = TimeUtil.localDatePart(todo.dueAt)
            dueDate != null && dueDate < today
        },
        highPriorityCount = pendingTodos.count { it.priority == "high" || it.isImportant == 1 },
        visibleTodos = pendingTodos
            .sortedWith(
                compareBy<TodoEntity> { TimeUtil.localDatePart(it.dueAt) == null }
                    .thenBy { TimeUtil.localDatePart(it.dueAt).orEmpty() }
                    .thenByDescending { it.priority == "high" || it.isImportant == 1 }
                    .thenBy { it.sortOrder }
            )
            .take(TODO_WIDGET_ITEM_LIMIT)
    )
}

fun Map<String, DailyAggregation>.toMonthWidgetState(): MonthWidgetState {
    val today = TimeUtil.getCurrentDate()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cells = mutableListOf<MonthWidgetDayCell>()

    repeat(firstDayOffset) {
        cells += MonthWidgetDayCell(day = null, date = null, isToday = false, aggregation = null)
    }

    for (day in 1..daysInMonth) {
        calendar.set(year, month, day)
        val date = formatter.format(calendar.time)
        cells += MonthWidgetDayCell(
            day = day,
            date = date,
            isToday = date == today,
            aggregation = this[date]
        )
    }

    while (cells.size % DAYS_PER_WEEK != 0) {
        cells += MonthWidgetDayCell(day = null, date = null, isToday = false, aggregation = null)
    }

    return MonthWidgetState(
        title = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time),
        cells = cells
    )
}

private const val TODO_WIDGET_ITEM_LIMIT = 5
private const val DAYS_PER_WEEK = 7
