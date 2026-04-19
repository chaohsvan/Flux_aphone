package com.example.flux.feature.todo.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.todo.ExportTodosUseCase
import com.example.flux.core.domain.todo.ToggleTodoStatusUseCase
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TodoStatusFilter {
    ALL,
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

enum class TodoPriorityFilter {
    ALL,
    NORMAL,
    HIGH
}

enum class TodoTimeFilter {
    ALL,
    TODAY,
    UPCOMING,
    OVERDUE,
    SCHEDULED,
    CUSTOM
}

data class TodoFilterState(
    val query: String = "",
    val status: TodoStatusFilter = TodoStatusFilter.ALL,
    val priority: TodoPriorityFilter = TodoPriorityFilter.ALL,
    val time: TodoTimeFilter = TodoTimeFilter.ALL,
    val customStartDate: String = "",
    val customEndDate: String = ""
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            status != TodoStatusFilter.ALL ||
            priority != TodoPriorityFilter.ALL ||
            time != TodoTimeFilter.ALL ||
            customStartDate.isNotBlank() ||
            customEndDate.isNotBlank()
}

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase,
    private val exportTodosUseCase: ExportTodosUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(TodoFilterState())
    val filterState = _filterState.asStateFlow()

    val todos: StateFlow<List<TodoEntity>> = combine(
        todoRepository.getActiveTodos(),
        _filterState
    ) { todos, filter ->
        todos.filter { todo -> todo.matches(filter) }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setStatusFilter(status: TodoStatusFilter) {
        _filterState.update { it.copy(status = status) }
    }

    fun setPriorityFilter(priority: TodoPriorityFilter) {
        _filterState.update { it.copy(priority = priority) }
    }

    fun setTimeFilter(time: TodoTimeFilter) {
        _filterState.update { it.copy(time = time) }
    }

    fun updateCustomStartDate(date: String) {
        _filterState.update { it.copy(customStartDate = date, time = TodoTimeFilter.CUSTOM) }
    }

    fun updateCustomEndDate(date: String) {
        _filterState.update { it.copy(customEndDate = date, time = TodoTimeFilter.CUSTOM) }
    }

    fun clearFilters() {
        _filterState.value = TodoFilterState()
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun selectAllVisible() {
        _selectedIds.value = todos.value.map { it.id }.toSet()
    }

    fun invertVisibleSelection() {
        val visibleIds = todos.value.map { it.id }.toSet()
        _selectedIds.update { current ->
            val selectedOutsideVisible = current - visibleIds
            val invertedVisible = visibleIds - current
            selectedOutsideVisible + invertedVisible
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

    fun exportSelectedToUri(context: Context, uri: Uri, format: TodoExportFormat) {
        viewModelScope.launch {
            exportTodosUseCase(context, _selectedIds.value, uri, format)
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

    private fun TodoEntity.matches(filter: TodoFilterState): Boolean {
        val query = filter.query.trim()
        val matchesQuery = query.isBlank() ||
            title.contains(query, ignoreCase = true) ||
            description.contains(query, ignoreCase = true) ||
            dueAt.orEmpty().contains(query, ignoreCase = true) ||
            startAt.orEmpty().contains(query, ignoreCase = true)

        val matchesStatus = when (filter.status) {
            TodoStatusFilter.ALL -> true
            TodoStatusFilter.PENDING -> status == "pending"
            TodoStatusFilter.IN_PROGRESS -> status == "in_progress"
            TodoStatusFilter.COMPLETED -> status == "completed"
        }

        val matchesPriority = when (filter.priority) {
            TodoPriorityFilter.ALL -> true
            TodoPriorityFilter.NORMAL -> priority != "high"
            TodoPriorityFilter.HIGH -> priority == "high"
        }

        val matchesTime = matchesTimeFilter(filter)
        return matchesQuery && matchesStatus && matchesPriority && matchesTime
    }

    private fun TodoEntity.matchesTimeFilter(filter: TodoFilterState): Boolean {
        val today = TimeUtil.getCurrentDate()
        val dueDate = dueAt?.takeIf { it.length >= 10 }?.take(10)

        return when (filter.time) {
            TodoTimeFilter.ALL -> true
            TodoTimeFilter.TODAY -> dueDate == today
            TodoTimeFilter.UPCOMING -> dueDate != null && dueDate >= today && status != "completed"
            TodoTimeFilter.OVERDUE -> dueDate != null && dueDate < today && status != "completed"
            TodoTimeFilter.SCHEDULED -> dueDate != null || startAt != null || reminderMinutes != null
            TodoTimeFilter.CUSTOM -> {
                val target = dueDate ?: startAt?.takeIf { it.length >= 10 }?.take(10)
                val afterStart = filter.customStartDate.isBlank() || (target != null && target >= filter.customStartDate)
                val beforeEnd = filter.customEndDate.isBlank() || (target != null && target <= filter.customEndDate)
                target != null && afterStart && beforeEnd
            }
        }
    }
}
