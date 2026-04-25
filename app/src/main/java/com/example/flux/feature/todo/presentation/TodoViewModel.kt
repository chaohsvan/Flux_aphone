package com.example.flux.feature.todo.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.core.domain.todo.ToggleTodoStatusUseCase
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.todo.domain.TodoFeatureGateway
import com.example.flux.feature.todo.domain.TodoFilterState
import com.example.flux.feature.todo.domain.TodoPriorityFilter
import com.example.flux.feature.todo.domain.TodoStats
import com.example.flux.feature.todo.domain.TodoStatusFilter
import com.example.flux.feature.todo.domain.TodoTimeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoUiState(
    val todos: List<TodoEntity> = emptyList(),
    val stats: TodoStats = TodoStats(),
    val projects: List<TodoProjectEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val filterState: TodoFilterState = TodoFilterState(),
    val trashSummary: TrashSummary = TrashSummary()
) {
    val hasStructuredFilters: Boolean
        get() = filterState.status != TodoStatusFilter.ALL ||
            filterState.priority != TodoPriorityFilter.ALL ||
            filterState.time != TodoTimeFilter.ALL ||
            filterState.projectId != null
}

private data class TodoContentState(
    val todos: List<TodoEntity> = emptyList(),
    val stats: TodoStats = TodoStats(),
    val projects: List<TodoProjectEntity> = emptyList()
)

private data class TodoSelectionState(
    val selectedIds: Set<String> = emptySet(),
    val filterState: TodoFilterState = TodoFilterState(),
    val trashSummary: TrashSummary = TrashSummary()
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoGateway: TodoFeatureGateway,
    private val toggleTodoStatusUseCase: ToggleTodoStatusUseCase,
    observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(TodoFilterState())

    val projects: StateFlow<List<TodoProjectEntity>> = todoGateway.getActiveProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeTodos: StateFlow<List<TodoEntity>> = todoGateway.getActiveTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<TodoStats> = activeTodos
        .map { todos -> todos.toStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoStats())

    val todos: StateFlow<List<TodoEntity>> = combine(
        activeTodos,
        _filterState
    ) { todos, filter ->
        todos.filter { todo -> todo.matches(filter) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val trashSummary: StateFlow<TrashSummary> = observeTrashSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrashSummary())

    private val contentState: StateFlow<TodoContentState> = combine(
        todos,
        stats,
        projects
    ) { todos, stats, projects ->
        TodoContentState(todos = todos, stats = stats, projects = projects)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoContentState())

    private val selectionState: StateFlow<TodoSelectionState> = combine(
        _selectedIds,
        _filterState,
        trashSummary
    ) { selectedIds, filterState, trashSummary ->
        TodoSelectionState(
            selectedIds = selectedIds,
            filterState = filterState,
            trashSummary = trashSummary
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoSelectionState())

    val uiState: StateFlow<TodoUiState> = combine(
        contentState,
        selectionState
    ) { contentState, selectionState ->
        TodoUiState(
            todos = contentState.todos,
            stats = contentState.stats,
            projects = contentState.projects,
            selectedIds = selectionState.selectedIds,
            filterState = selectionState.filterState,
            trashSummary = selectionState.trashSummary
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

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
            todoGateway.createProject(trimmed)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            todoGateway.softDeleteProject(projectId)
            _filterState.update { state ->
                if (state.projectId == projectId) state.copy(projectId = null) else state
            }
        }
    }

    fun batchDelete() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id -> todoGateway.softDeleteTodo(id) }
            clearSelection()
        }
    }

    fun batchMarkHighPriority() {
        viewModelScope.launch {
            todoGateway.updatePriority(_selectedIds.value, "high")
            clearSelection()
        }
    }

    fun batchMarkNormalPriority() {
        viewModelScope.launch {
            todoGateway.updatePriority(_selectedIds.value, "normal")
            clearSelection()
        }
    }

    fun exportSelectedToUri(context: Context, uri: Uri, format: TodoExportFormat) {
        viewModelScope.launch {
            todoGateway.exportSelected(context, _selectedIds.value, uri, format)
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
            todoGateway.reorderTodos(list)
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
            val now = TimeUtil.getCurrentIsoTime()
            val newTodo = TodoEntity(
                id = TimeUtil.generateUuid(),
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
            todoGateway.saveTodoWithHistory(newTodo, "create", "创建待办")
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
