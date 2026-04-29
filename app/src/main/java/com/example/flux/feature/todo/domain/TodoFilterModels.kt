package com.example.flux.feature.todo.domain

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

