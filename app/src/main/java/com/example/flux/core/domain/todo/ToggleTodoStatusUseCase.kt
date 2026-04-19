package com.example.flux.core.domain.todo

import com.example.flux.core.database.repository.TodoRepository
import javax.inject.Inject

class ToggleTodoStatusUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(id: String, currentStatus: String) {
        val newStatus = if (currentStatus == "completed") "pending" else "completed"
        todoRepository.updateTodoStatus(id, newStatus)
    }
}
