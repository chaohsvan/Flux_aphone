package com.example.flux.feature.todo.data

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.todo.ExportTodosUseCase
import com.example.flux.core.domain.todo.TodoExportFormat
import com.example.flux.feature.todo.domain.TodoFeatureGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultTodoFeatureGateway @Inject constructor(
    private val todoRepository: TodoRepository,
    private val exportTodosUseCase: ExportTodosUseCase
) : TodoFeatureGateway {

    override fun getActiveTodos(): Flow<List<TodoEntity>> = todoRepository.getActiveTodos()

    override fun getActiveProjects(): Flow<List<TodoProjectEntity>> = todoRepository.getActiveProjects()

    override fun getHistoryForTodo(todoId: String): Flow<List<TodoHistoryEntity>> = todoRepository.getHistoryForTodo(todoId)

    override fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>> = todoRepository.getSubtasksForTodo(todoId)

    override suspend fun getTodoById(id: String): TodoEntity? = todoRepository.getTodoById(id)

    override suspend fun createProject(name: String): TodoProjectEntity = todoRepository.createProject(name)

    override suspend fun softDeleteProject(projectId: String) {
        todoRepository.softDeleteProject(projectId)
    }

    override suspend fun saveTodoWithHistory(todo: TodoEntity, action: String, summary: String, payloadJson: String) {
        todoRepository.saveTodoWithHistory(todo, action, summary, payloadJson)
    }

    override suspend fun updatePriority(ids: Set<String>, priority: String) {
        todoRepository.updatePriority(ids, priority)
    }

    override suspend fun reorderTodos(orderedTodos: List<TodoEntity>) {
        todoRepository.reorderTodos(orderedTodos)
    }

    override suspend fun softDeleteTodo(id: String) {
        todoRepository.softDeleteTodo(id)
    }

    override suspend fun saveSubtask(subtask: TodoSubtaskEntity) {
        todoRepository.saveSubtask(subtask)
    }

    override suspend fun updateSubtaskCompleted(id: String, isCompleted: Boolean, updatedAt: String) {
        todoRepository.updateSubtaskCompleted(id, isCompleted, updatedAt)
    }

    override suspend fun reorderSubtasks(todoId: String, orderedSubtasks: List<TodoSubtaskEntity>) {
        todoRepository.reorderSubtasks(todoId, orderedSubtasks)
    }

    override suspend fun deleteSubtask(id: String) {
        todoRepository.deleteSubtask(id)
    }

    override suspend fun exportSelected(context: Context, ids: Set<String>, uri: Uri, format: TodoExportFormat) {
        exportTodosUseCase(context, ids, uri, format)
    }
}
