package com.example.flux.core.domain.todo

import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.util.TimeUtil
import javax.inject.Inject

class RestoreTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(id: String) {
        val todo = todoRepository.getTodoById(id)
        if (todo != null && todo.deletedAt != null) {
            val restored = todo.copy(
                deletedAt = null,
                updatedAt = TimeUtil.getCurrentIsoTime(),
                version = todo.version + 1
            )
            todoRepository.saveTodoWithHistory(restored, "restore", "从回收站恢复")
        }
    }
}
