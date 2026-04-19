package com.example.flux.feature.todo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoDetailUiState(
    val todo: TodoEntity? = null,
    val title: String = "",
    val description: String = "",
    val priority: String = "normal",
    val status: String = "pending",
    val startAt: String = "",
    val dueAt: String = "",
    val reminderMinutesText: String = "",
    val subtasks: List<TodoSubtaskEntity> = emptyList(),
    val isFormInitialized: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class TodoDetailViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val todoId: String? = savedStateHandle["todoId"]

    private val _uiState = MutableStateFlow(TodoDetailUiState(isLoading = true))
    val uiState: StateFlow<TodoDetailUiState> = _uiState.asStateFlow()

    init {
        if (todoId != null) {
            loadData(todoId)
        }
    }

    private fun loadData(id: String) {
        viewModelScope.launch {
            // Wait, we don't have getTodoById as a Flow. Let's assume we fetch it once or listen to subtasks.
            // Actually, for simplicity let's just listen to subtasks as Flow and fetch Todo once.
            val todo = todoRepository.getTodoById(id)
            if (todo != null) {
                todoRepository.getSubtasksForTodo(id).collect { subtasks ->
                    _uiState.update { 
                        it.copy(
                            todo = todo,
                            title = if (it.isFormInitialized) it.title else todo.title,
                            description = if (it.isFormInitialized) it.description else todo.description,
                            priority = if (it.isFormInitialized) it.priority else todo.priority,
                            status = if (it.isFormInitialized) it.status else todo.status,
                            startAt = if (it.isFormInitialized) it.startAt else todo.startAt.orEmpty(),
                            dueAt = if (it.isFormInitialized) it.dueAt else todo.dueAt.orEmpty(),
                            reminderMinutesText = if (it.isFormInitialized) it.reminderMinutesText else todo.reminderMinutes?.toString().orEmpty(),
                            subtasks = subtasks,
                            isFormInitialized = true,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateStartAt(value: String) {
        _uiState.update { it.copy(startAt = value) }
    }

    fun updateDueAt(value: String) {
        _uiState.update { it.copy(dueAt = value) }
    }

    fun updateReminderMinutes(value: String) {
        _uiState.update { state ->
            state.copy(reminderMinutesText = value.filter { it.isDigit() }.take(5))
        }
    }

    fun setPriority(priority: String) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun setStatus(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun saveTodo() {
        val state = _uiState.value
        val currentTodo = state.todo ?: return
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val completedAt = when {
                state.status == "completed" && currentTodo.completedAt == null -> now
                state.status != "completed" -> null
                else -> currentTodo.completedAt
            }
            val updated = currentTodo.copy(
                title = state.title.trim(),
                description = state.description.trim(),
                status = state.status,
                priority = state.priority,
                dueAt = state.dueAt.trim().ifBlank { null },
                startAt = state.startAt.trim().ifBlank { null },
                completedAt = completedAt,
                isImportant = if (state.priority == "high") 1 else 0,
                reminderMinutes = state.reminderMinutesText.toIntOrNull(),
                updatedAt = now,
                version = currentTodo.version + 1
            )
            todoRepository.saveTodo(updated)
            _uiState.update { it.copy(todo = updated) }
        }
    }

    fun addSubtask(title: String) {
        val currentTodoId = todoId ?: return
        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val newSubtask = TodoSubtaskEntity(
                id = TimeUtil.generateUuid(),
                todoId = currentTodoId,
                title = title,
                isCompleted = 0,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            todoRepository.saveSubtask(newSubtask)
        }
    }

    fun toggleSubtaskStatus(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.updateSubtaskCompleted(subtaskId, !isCompleted, TimeUtil.getCurrentIsoTime())
        }
    }

    fun deleteTodo() {
        val currentTodoId = todoId ?: return
        viewModelScope.launch {
            todoRepository.softDeleteTodo(currentTodoId)
        }
    }
}
