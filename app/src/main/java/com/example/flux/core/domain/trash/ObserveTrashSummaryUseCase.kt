package com.example.flux.core.domain.trash

import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.TodoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class TrashSummary(
    val diaries: Int = 0,
    val todos: Int = 0,
    val events: Int = 0
) {
    val total: Int
        get() = diaries + todos + events
}

class ObserveTrashSummaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<TrashSummary> {
        return combine(
            diaryRepository.getDeletedDiaries(),
            todoRepository.getDeletedTodos(),
            eventRepository.getDeletedEvents()
        ) { diaries, todos, events ->
            TrashSummary(
                diaries = diaries.size,
                todos = todos.size,
                events = events.size
            )
        }
    }
}
