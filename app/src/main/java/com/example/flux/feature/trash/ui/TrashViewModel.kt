package com.example.flux.feature.trash.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.calendar.RestoreEventUseCase
import com.example.flux.core.domain.diary.RestoreDiaryUseCase
import com.example.flux.core.domain.todo.RestoreTodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    diaryRepository: DiaryRepository,
    todoRepository: TodoRepository,
    eventRepository: EventRepository,
    private val restoreDiaryUseCase: RestoreDiaryUseCase,
    private val restoreTodoUseCase: RestoreTodoUseCase,
    private val restoreEventUseCase: RestoreEventUseCase
) : ViewModel() {

    val deletedDiaries: StateFlow<List<DiaryEntity>> = diaryRepository.getDeletedDiaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTodos: StateFlow<List<TodoEntity>> = todoRepository.getDeletedTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedEvents: StateFlow<List<CalendarEventEntity>> = eventRepository.getDeletedEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreDiary(id: String) {
        viewModelScope.launch {
            restoreDiaryUseCase(id)
        }
    }

    fun restoreTodo(id: String) {
        viewModelScope.launch {
            restoreTodoUseCase(id)
        }
    }

    fun restoreEvent(id: String) {
        viewModelScope.launch {
            restoreEventUseCase(id)
        }
    }
}
