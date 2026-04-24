package com.example.flux.feature.todo.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.todo.ExportTodosUseCase
import com.example.flux.core.domain.todo.ToggleTodoStatusUseCase
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    val projectId: String? = null,
    val customStartDate: String = "",
    val customEndDate: String = ""
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            status != TodoStatusFilter.ALL ||
            priority != TodoPriorityFilter.ALL ||
            time != TodoTimeFilter.ALL ||
            projectId != null ||
            customStartDate.isNotBlank() ||
            customEndDate.isNotBlank()
}

data class TodoStats(
    val total: Int = 0,
    val today: Int = 0,
    val overdue: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val highPriority: Int = 0
) {
    val completionPercent: Int
        get() = if (total == 0) 0 else ((completed * 100.0) / total).toInt()
}

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase,
    private val exportTodosUseCase: ExportTodosUseCase,
    observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(TodoFilterState())
    val filterState = _filterState.asStateFlow()

    val projects: StateFlow<List<TodoProjectEntity>> = todoRepository.getActiveProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val activeTodos: StateFlow<List<TodoEntity>> = todoRepository.getActiveTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stats: StateFlow<TodoStats> = activeTodos
        .map { todos -> todos.toStats() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodoStats()
        )

    val todos: StateFlow<List<TodoEntity>> = combine(
        activeTodos,
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

    val trashSummary: StateFlow<TrashSummary> = observeTrashSummaryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

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

    fun setProjectFilter(projectId: String?) {
        _filterState.update { it.copy(projectId = projectId) }
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

    fun addProject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            todoRepository.createProject(trimmed)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            todoRepository.softDeleteProject(projectId)
            _filterState.update { state ->
                if (state.projectId == projectId) state.copy(projectId = null) else state
            }
        }
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
        val index = todos.value.indexOfFirst { it.id == id }
        val newIndex = if (moveUp) index - 1 else index + 1
        moveTodoToIndex(id, newIndex)
    }

    fun moveTodoToIndex(id: String, targetIndex: Int) {
        viewModelScope.launch {
            val list = todos.value.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index == -1 || targetIndex !in list.indices || index == targetIndex) return@launch

            val moved = list.removeAt(index)
            list.add(targetIndex, moved)
            todoRepository.reorderTodos(list)
        }
    }

    fun toggleStatus(id: String, currentStatus: String) {
        viewModelScope.launch {
            toggleTodoStatusUseCase(id, currentStatus)
        }
    }

    fun addTodo(title: String, description: String, priority: String, projectId: String?, dueAt: String = "") {
        if (!TimeUtil.isValidDateOrDateTime(dueAt.trim())) return
        viewModelScope.launch {
            val now = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
            val newTodo = TodoEntity(
                id = com.example.flux.core.util.TimeUtil.generateUuid(),
                projectId = projectId,
                title = title,
                description = description,
                status = "pending",
                priority = priority,
                dueAt = dueAt.ifBlank { null },
                startAt = null,
                completedAt = null,
                sortOrder = activeTodos.value.size,
                isImportant = if (priority == "high") 1 else 0,
                reminderMinutes = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            todoRepository.saveTodoWithHistory(newTodo, "create", "创建待办")
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

        val matchesProject = filter.projectId == null || projectId == filter.projectId
        val matchesTime = matchesTimeFilter(filter)
        return matchesQuery && matchesStatus && matchesPriority && matchesProject && matchesTime
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

    private fun List<TodoEntity>.toStats(): TodoStats {
        val today = TimeUtil.getCurrentDate()
        return TodoStats(
            total = size,
            today = count { it.dueAt?.takeIf { due -> due.length >= 10 }?.take(10) == today },
            overdue = count {
                val dueDate = it.dueAt?.takeIf { due -> due.length >= 10 }?.take(10)
                dueDate != null && dueDate < today && it.status != "completed"
            },
            inProgress = count { it.status == "in_progress" },
            completed = count { it.status == "completed" },
            highPriority = count { it.priority == "high" }
        )
    }
}
