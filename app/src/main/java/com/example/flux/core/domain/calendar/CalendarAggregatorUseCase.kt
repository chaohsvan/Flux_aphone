package com.example.flux.core.domain.calendar

import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.dao.TodoDao
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.core.util.TimeUtil
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DailyAggregation(
    val date: String, // YYYY-MM-DD
    val hasDiary: Boolean,
    val pendingTodosCount: Int,
    val completedTodosCount: Int,
    val eventColors: List<String>,
    val deletedCount: Int = 0
)

class CalendarAggregatorUseCase @Inject constructor(
    private val diaryDao: DiaryDao,
    private val todoDao: TodoDao,
    private val eventDao: EventDao
) {
    operator fun invoke(): Flow<Map<String, DailyAggregation>> {
        val activeItems = combine(
            diaryDao.getActiveDiaries(),
            todoDao.getActiveTodos(),
            eventDao.getActiveEvents()
        ) { diaries, todos, events ->
            Triple(diaries, todos, events)
        }
        val deletedItems = combine(
            diaryDao.getDeletedDiaries(),
            todoDao.getDeletedTodos(),
            eventDao.getDeletedEvents()
        ) { diaries, todos, events ->
            Triple(diaries, todos, events)
        }
        return combine(activeItems, deletedItems) { active, deleted ->
            val (diaries, todos, events) = active
            val (deletedDiaries, deletedTodos, deletedEvents) = deleted
            val result = mutableMapOf<String, DailyAggregation>()
            val range = aggregationRange()
            
            diaries.forEach { diary ->
                val date = diary.entryDate
                result[date] = DailyAggregation(date, true, 0, 0, emptyList())
            }
            
            todos.forEach { todo ->
                if (todo.status == "completed") return@forEach
                todo.calendarOccurrenceDates(range.first, range.second).forEach { date ->
                    val current = result[date] ?: DailyAggregation(date, false, 0, 0, emptyList())
                    result[date] = current.copy(pendingTodosCount = current.pendingTodosCount + 1)
                }
            }
            
            events.forEach { event ->
                RecurrenceUtil.occurrenceDates(
                    value = event.startAt,
                    recurrence = event.recurrenceRule,
                    rangeStart = range.first,
                    rangeEnd = range.second
                ).forEach { date ->
                    val current = result[date] ?: DailyAggregation(date, false, 0, 0, emptyList())
                    val newColors = current.eventColors.toMutableList()
                    if (event.color != null) newColors.add(event.color)
                    result[date] = current.copy(eventColors = newColors)
                }
            }

            deletedDiaries.forEach { diary ->
                result.addDeletedMarker(diary.entryDate)
            }
            deletedTodos.forEach { todo ->
                result.addDeletedMarker(TimeUtil.localDatePart(todo.dueAt).orEmpty())
            }
            deletedEvents.forEach { event ->
                result.addDeletedMarker(event.startAt.take(10))
            }
            
            result
        }
    }

    private fun MutableMap<String, DailyAggregation>.addDeletedMarker(date: String) {
        if (date.length < 10) return
        val current = this[date] ?: DailyAggregation(date, false, 0, 0, emptyList())
        this[date] = current.copy(deletedCount = current.deletedCount + 1)
    }

    private fun aggregationRange(): Pair<String, String> {
        val currentYear = TimeUtil.getCurrentDate().take(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        return "${currentYear - 2}-01-01" to "${currentYear + 3}-12-31"
    }
}
