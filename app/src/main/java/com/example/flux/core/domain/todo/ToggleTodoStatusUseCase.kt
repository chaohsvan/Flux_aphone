package com.example.flux.core.domain.todo

import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.util.TimeUtil
import javax.inject.Inject

class ToggleTodoStatusUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(id: String, currentStatus: String) {
        val todo = todoRepository.getTodoById(id) ?: return
        val now = TimeUtil.getCurrentIsoTime()
        val newStatus = if (currentStatus == "completed") "pending" else "completed"
        todoRepository.saveTodo(
            todo.copy(
                status = newStatus,
                completedAt = if (newStatus == "completed") now else null,
                updatedAt = now,
                version = todo.version + 1
            )
        )
    }
}
