package com.example.flux.feature.todo.domain

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.domain.todo.TodoExportFormat
import kotlinx.coroutines.flow.Flow

interface TodoFeatureGateway {
    fun getActiveTodos(): Flow<List<TodoEntity>>
    fun getActiveProjects(): Flow<List<TodoProjectEntity>>
    fun getHistoryForTodo(todoId: String): Flow<List<TodoHistoryEntity>>
    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>>

    suspend fun getTodoById(id: String): TodoEntity?
    suspend fun createProject(name: String): TodoProjectEntity
    suspend fun softDeleteProject(projectId: String)
    suspend fun saveTodoWithHistory(todo: TodoEntity, action: String, summary: String, payloadJson: String = "{}")
    suspend fun updatePriority(ids: Set<String>, priority: String)
    suspend fun reorderTodos(orderedTodos: List<TodoEntity>)
    suspend fun softDeleteTodo(id: String)
    suspend fun saveSubtask(subtask: TodoSubtaskEntity)
    suspend fun updateSubtaskCompleted(id: String, isCompleted: Boolean, updatedAt: String)
    suspend fun reorderSubtasks(todoId: String, orderedSubtasks: List<TodoSubtaskEntity>)
    suspend fun deleteSubtask(id: String)
    suspend fun exportSelected(context: Context, ids: Set<String>, uri: Uri, format: TodoExportFormat)
}
