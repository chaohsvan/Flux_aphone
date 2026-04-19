package com.example.flux.core.domain.calendar

import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.dao.TodoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DailyAggregation(
    val date: String, // YYYY-MM-DD
    val hasDiary: Boolean,
    val pendingTodosCount: Int,
    val completedTodosCount: Int,
    val eventColors: List<String>
)

class CalendarAggregatorUseCase @Inject constructor(
    private val diaryDao: DiaryDao,
    private val todoDao: TodoDao,
    private val eventDao: EventDao
) {
    operator fun invoke(): Flow<Map<String, DailyAggregation>> {
        return combine(
            diaryDao.getActiveDiaries(),
            todoDao.getActiveTodos(),
            eventDao.getActiveEvents()
        ) { diaries, todos, events ->
            val result = mutableMapOf<String, DailyAggregation>()
            
            diaries.forEach { diary ->
                val date = diary.entryDate
                result[date] = DailyAggregation(date, true, 0, 0, emptyList())
            }
            
            todos.forEach { todo ->
                val rawDate = todo.dueAt ?: todo.createdAt
                if (rawDate.length >= 10) {
                    val date = rawDate.take(10)
                    val current = result[date] ?: DailyAggregation(date, false, 0, 0, emptyList())
                    if (todo.status == "completed") {
                        result[date] = current.copy(completedTodosCount = current.completedTodosCount + 1)
                    } else {
                        result[date] = current.copy(pendingTodosCount = current.pendingTodosCount + 1)
                    }
                }
            }
            
            events.forEach { event ->
                if (event.startAt.length >= 10) {
                    val date = event.startAt.take(10)
                    val current = result[date] ?: DailyAggregation(date, false, 0, 0, emptyList())
                    val newColors = current.eventColors.toMutableList()
                    if (event.color != null) newColors.add(event.color)
                    result[date] = current.copy(eventColors = newColors)
                }
            }
            
            result
        }
    }
}
