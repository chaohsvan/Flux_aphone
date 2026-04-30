package com.example.flux.feature.trash.data

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.calendar.RestoreEventUseCase
import com.example.flux.core.domain.diary.RestoreDiaryUseCase
import com.example.flux.core.domain.todo.RestoreTodoUseCase
import com.example.flux.feature.trash.domain.TrashFeatureGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultTrashFeatureGateway @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository,
    private val restoreDiaryUseCase: RestoreDiaryUseCase,
    private val restoreTodoUseCase: RestoreTodoUseCase,
    private val restoreEventUseCase: RestoreEventUseCase
) : TrashFeatureGateway {

    override fun getDeletedDiaries(): Flow<List<DiaryEntity>> = diaryRepository.getDeletedDiaries()

    override fun getDeletedTodos(): Flow<List<TodoEntity>> = todoRepository.getDeletedTodos()

    override fun getDeletedEvents(): Flow<List<CalendarEventEntity>> = eventRepository.getDeletedEvents()

    override suspend fun restoreDiary(id: String) {
        restoreDiaryUseCase(id)
    }

    override suspend fun restoreTodo(id: String) {
        restoreTodoUseCase(id)
    }

    override suspend fun restoreEvent(id: String) {
        restoreEventUseCase(id)
    }

    override suspend fun permanentlyDeleteDiary(id: String) {
        diaryRepository.permanentlyDeleteDiary(id)
    }

    override suspend fun permanentlyDeleteTodo(id: String) {
        todoRepository.permanentlyDeleteTodo(id)
    }

    override suspend fun permanentlyDeleteEvent(id: String) {
        eventRepository.permanentlyDeleteEvent(id)
    }
}
