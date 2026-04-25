package com.example.flux.core.domain.todo

import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.core.util.TimeUtil
import javax.inject.Inject

class CreateNextRecurringTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(completedTodo: TodoEntity) {
        if (completedTodo.status != "completed" || completedTodo.recurrence == "none") return

        val interval = completedTodo.recurrenceInterval.coerceAtLeast(1)
        val nextDueAt = completedTodo.dueAt?.let {
            RecurrenceUtil.nextValue(it, completedTodo.recurrence, interval)
        }
        val nextStartAt = completedTodo.startAt?.let {
            RecurrenceUtil.nextValue(it, completedTodo.recurrence, interval)
        }
        val occurrenceDate = (nextDueAt ?: nextStartAt)?.take(10) ?: return
        val until = completedTodo.recurrenceUntil
        if (until != null && occurrenceDate > until) return

        val parentId = completedTodo.parentTodoId ?: completedTodo.id
        if (todoRepository.getActiveRecurringChild(parentId, nextDueAt, nextStartAt) != null) return

        val now = TimeUtil.getCurrentIsoTime()
        val nextTodo = completedTodo.copy(
            id = TimeUtil.generateUuid(),
            status = "pending",
            completedAt = null,
            dueAt = nextDueAt,
            startAt = nextStartAt,
            parentTodoId = parentId,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            version = 1
        )
        todoRepository.saveTodoWithHistory(nextTodo, "recurrence_create", "生成下一轮待办")
        todoRepository.copySubtasksForRecurringTodo(completedTodo.id, nextTodo.id, now)
        todoRepository.addHistory(completedTodo.id, "recurrence_next", "已生成下一轮：$occurrenceDate")
    }
}
