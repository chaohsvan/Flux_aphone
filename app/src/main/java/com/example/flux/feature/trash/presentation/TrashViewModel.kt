package com.example.flux.feature.trash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.feature.trash.domain.TrashFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val deletedDiaries: List<DiaryEntity> = emptyList(),
    val deletedTodos: List<TodoEntity> = emptyList(),
    val deletedEvents: List<CalendarEventEntity> = emptyList()
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashFeatureGateway: TrashFeatureGateway
) : ViewModel() {

    val deletedDiaries: StateFlow<List<DiaryEntity>> = trashFeatureGateway.getDeletedDiaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTodos: StateFlow<List<TodoEntity>> = trashFeatureGateway.getDeletedTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedEvents: StateFlow<List<CalendarEventEntity>> = trashFeatureGateway.getDeletedEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<TrashUiState> = combine(
        deletedDiaries,
        deletedTodos,
        deletedEvents
    ) { deletedDiaries, deletedTodos, deletedEvents ->
        TrashUiState(
            deletedDiaries = deletedDiaries,
            deletedTodos = deletedTodos,
            deletedEvents = deletedEvents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState()
    )

    fun restoreDiary(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.restoreDiary(id)
        }
    }

    fun restoreTodo(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.restoreTodo(id)
        }
    }

    fun restoreEvent(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.restoreEvent(id)
        }
    }

    fun permanentlyDeleteDiary(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.permanentlyDeleteDiary(id)
        }
    }

    fun permanentlyDeleteTodo(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.permanentlyDeleteTodo(id)
        }
    }

    fun permanentlyDeleteEvent(id: String) {
        viewModelScope.launch {
            trashFeatureGateway.permanentlyDeleteEvent(id)
        }
    }
}
