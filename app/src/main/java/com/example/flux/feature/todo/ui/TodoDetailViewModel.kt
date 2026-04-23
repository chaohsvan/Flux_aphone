package com.example.flux.feature.todo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoDetailUiState(
    val todo: TodoEntity? = null,
    val title: String = "",
    val description: String = "",
    val priority: String = "normal",
    val status: String = "pending",
    val projectId: String? = null,
    val startAt: String = "",
    val dueAt: String = "",
    val reminderMinutesText: String = "",
    val projects: List<TodoProjectEntity> = emptyList(),
    val history: List<TodoHistoryEntity> = emptyList(),
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
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadData(id: String) {
        viewModelScope.launch {
            val todo = todoRepository.getTodoById(id)
            if (todo == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                todoRepository.getSubtasksForTodo(id),
                todoRepository.getActiveProjects(),
                todoRepository.getHistoryForTodo(id)
            ) { subtasks, projects, history ->
                Triple(subtasks, projects, history)
            }.collect { (subtasks, projects, history) ->
                _uiState.update { state ->
                    state.copy(
                        todo = state.todo ?: todo,
                        title = if (state.isFormInitialized) state.title else todo.title,
                        description = if (state.isFormInitialized) state.description else todo.description,
                        priority = if (state.isFormInitialized) state.priority else todo.priority,
                        status = if (state.isFormInitialized) state.status else todo.status,
                        projectId = if (state.isFormInitialized) state.projectId else todo.projectId,
                        startAt = if (state.isFormInitialized) state.startAt else todo.startAt.orEmpty(),
                        dueAt = if (state.isFormInitialized) state.dueAt else todo.dueAt.orEmpty(),
                        reminderMinutesText = if (state.isFormInitialized) {
                            state.reminderMinutesText
                        } else {
                            todo.reminderMinutes?.toString().orEmpty()
                        },
                        projects = projects,
                        history = history,
                        subtasks = subtasks,
                        isFormInitialized = true,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateProjectId(value: String?) {
        _uiState.update { it.copy(projectId = value) }
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
                projectId = state.projectId,
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
            todoRepository.saveTodoWithHistory(updated, "edit", "更新待办详情")
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
