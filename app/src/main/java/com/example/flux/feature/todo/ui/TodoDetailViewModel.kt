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
    val subtasks: List<TodoSubtaskEntity> = emptyList(),
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
                        it.copy(todo = todo, subtasks = subtasks, isLoading = false) 
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
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
