package com.example.flux.feature.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.todo.ToggleTodoStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase
) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = todoRepository.getActiveTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            ids.forEach { id ->
                todoRepository.softDeleteTodo(id)
            }
            clearSelection()
        }
    }

    fun batchMarkHighPriority() {
        viewModelScope.launch {
            todoRepository.updatePriority(_selectedIds.value, "high")
            clearSelection()
        }
    }

    fun batchMarkNormalPriority() {
        viewModelScope.launch {
            todoRepository.updatePriority(_selectedIds.value, "normal")
            clearSelection()
        }
    }

    fun moveTodoOrder(id: String, moveUp: Boolean) {
        viewModelScope.launch {
            val list = todos.value.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index == -1) return@launch

            val newIndex = if (moveUp) index - 1 else index + 1
            if (newIndex !in list.indices) return@launch

            // Swap in memory
            val temp = list[index]
            list[index] = list[newIndex]
            list[newIndex] = temp

            // Re-assign sortOrders (smaller is higher)
            list.forEachIndexed { i, todo ->
                val newSortOrder = i
                if (todo.sortOrder != newSortOrder) {
                    todoRepository.saveTodo(todo.copy(sortOrder = newSortOrder))
                }
            }
        }
    }

    fun toggleStatus(id: String, currentStatus: String) {
        viewModelScope.launch {
            toggleTodoStatusUseCase(id, currentStatus)
        }
    }

    fun addTodo(title: String, description: String, priority: String) {
        viewModelScope.launch {
            val now = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
            val newTodo = TodoEntity(
                id = com.example.flux.core.util.TimeUtil.generateUuid(),
                projectId = null,
                title = title,
                description = description,
                status = "pending",
                priority = priority,
                dueAt = null,
                startAt = null,
                completedAt = null,
                sortOrder = 0,
                isImportant = if (priority == "high") 1 else 0,
                reminderMinutes = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            todoRepository.saveTodo(newTodo)
        }
    }
}
