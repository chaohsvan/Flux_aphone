package com.example.flux.feature.todo.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.domain.todo.CreateNextRecurringTodoUseCase
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.todo.domain.TodoFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val recurrence: String = "none",
    val recurrenceIntervalText: String = "1",
    val recurrenceUntil: String = "",
    val projects: List<TodoProjectEntity> = emptyList(),
    val history: List<TodoHistoryEntity> = emptyList(),
    val subtasks: List<TodoSubtaskEntity> = emptyList(),
    val isFormInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TodoDetailViewModel @Inject constructor(
    private val todoGateway: TodoFeatureGateway,
    private val createNextRecurringTodoUseCase: CreateNextRecurringTodoUseCase,
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
            val todo = todoGateway.getTodoById(id)
            if (todo == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                todoGateway.getSubtasksForTodo(id),
                todoGateway.getActiveProjects(),
                todoGateway.getHistoryForTodo(id)
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
                        recurrence = if (state.isFormInitialized) state.recurrence else todo.recurrence,
                        recurrenceIntervalText = if (state.isFormInitialized) {
                            state.recurrenceIntervalText
                        } else {
                            todo.recurrenceInterval.coerceAtLeast(1).toString()
                        },
                        recurrenceUntil = if (state.isFormInitialized) {
                            state.recurrenceUntil
                        } else {
                            todo.recurrenceUntil.orEmpty()
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
        _uiState.update { it.copy(startAt = value, errorMessage = null) }
    }

    fun updateDueAt(value: String) {
        _uiState.update { it.copy(dueAt = value, errorMessage = null) }
    }

    fun updateReminderMinutes(value: String) {
        _uiState.update { it.copy(reminderMinutesText = value.filter(Char::isDigit).take(5), errorMessage = null) }
    }

    fun setRecurrence(value: String) {
        _uiState.update { state ->
            state.copy(
                recurrence = value,
                recurrenceIntervalText = if (value == "none") "1" else state.recurrenceIntervalText,
                recurrenceUntil = if (value == "none") "" else state.recurrenceUntil,
                errorMessage = null
            )
        }
    }

    fun updateRecurrenceInterval(value: String) {
        _uiState.update { it.copy(recurrenceIntervalText = value.filter(Char::isDigit).take(3), errorMessage = null) }
    }

    fun updateRecurrenceUntil(value: String) {
        _uiState.update { it.copy(recurrenceUntil = value, errorMessage = null) }
    }

    fun setPriority(priority: String) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun setStatus(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun saveTodo(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val currentTodo = state.todo ?: return
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "任务内容不能为空") }
            return
        }
        if (!TimeUtil.isValidDateOrDateTime(state.startAt.trim()) || !TimeUtil.isValidDateOrDateTime(state.dueAt.trim())) {
            _uiState.update { it.copy(errorMessage = "日期格式应为 YYYY-MM-DD 或 YYYY-MM-DD HH:mm") }
            return
        }
        if (state.recurrence !in SUPPORTED_RECURRENCES) {
            _uiState.update { it.copy(errorMessage = "不支持的重复规则") }
            return
        }
        if (state.recurrenceUntil.isNotBlank() && !TimeUtil.isValidDate(state.recurrenceUntil.trim())) {
            _uiState.update { it.copy(errorMessage = "重复截止日期应为 YYYY-MM-DD") }
            return
        }

        val recurrenceInterval = state.recurrenceIntervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
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
                recurrence = state.recurrence,
                recurrenceInterval = if (state.recurrence == "none") 1 else recurrenceInterval,
                recurrenceUntil = state.recurrenceUntil.trim().ifBlank { null },
                updatedAt = now,
                version = currentTodo.version + 1
            )
            todoGateway.saveTodoWithHistory(updated, "edit", "更新待办详情")
            if (currentTodo.status != "completed" && updated.status == "completed") {
                createNextRecurringTodoUseCase(updated)
            }
            _uiState.update { it.copy(todo = updated) }
            onSaved()
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
                sortOrder = _uiState.value.subtasks.size,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            todoGateway.saveSubtask(newSubtask)
        }
    }

    fun moveSubtaskOrder(subtaskId: String, moveUp: Boolean) {
        val currentTodoId = todoId ?: return
        viewModelScope.launch {
            val list = _uiState.value.subtasks.toMutableList()
            val index = list.indexOfFirst { it.id == subtaskId }
            if (index == -1) return@launch

            val newIndex = if (moveUp) index - 1 else index + 1
            if (newIndex !in list.indices) return@launch

            val item = list.removeAt(index)
            list.add(newIndex, item)
            todoGateway.reorderSubtasks(currentTodoId, list)
        }
    }

    fun toggleSubtaskStatus(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            todoGateway.updateSubtaskCompleted(subtaskId, !isCompleted, TimeUtil.getCurrentIsoTime())
        }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch {
            todoGateway.deleteSubtask(subtaskId)
        }
    }

    fun deleteTodo() {
        val currentTodoId = todoId ?: return
        viewModelScope.launch {
            todoGateway.softDeleteTodo(currentTodoId)
        }
    }

    companion object {
        private val SUPPORTED_RECURRENCES = setOf("none", "daily", "weekly", "monthly", "yearly")
    }
}
