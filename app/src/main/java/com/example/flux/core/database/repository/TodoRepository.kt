package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.TodoDao
import com.example.flux.core.database.dao.TodoHistoryDao
import com.example.flux.core.database.dao.TodoProjectDao
import com.example.flux.core.database.dao.TodoSubtaskDao
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val todoSubtaskDao: TodoSubtaskDao,
    private val todoProjectDao: TodoProjectDao,
    private val todoHistoryDao: TodoHistoryDao
) {
    fun getActiveTodos(): Flow<List<TodoEntity>> {
        return todoDao.getActiveTodos()
    }

    fun getTodosByDate(date: String): Flow<List<TodoEntity>> {
        return todoDao.getTodosByDate(date)
    }

    fun getDeletedTodos(): Flow<List<TodoEntity>> {
        return todoDao.getDeletedTodos()
    }

    fun getActiveProjects(): Flow<List<TodoProjectEntity>> {
        return todoProjectDao.getActiveProjects()
    }

    fun getHistoryForTodo(todoId: String): Flow<List<TodoHistoryEntity>> {
        return todoHistoryDao.getHistoryForTodo(todoId)
    }

    suspend fun getTodoById(id: String): TodoEntity? {
        return todoDao.getTodoById(id)
    }

    suspend fun saveProject(project: TodoProjectEntity) {
        todoProjectDao.insertProject(project)
    }

    suspend fun createProject(name: String): TodoProjectEntity {
        val now = TimeUtil.getCurrentIsoTime()
        val project = TodoProjectEntity(
            id = TimeUtil.generateUuid(),
            name = name.trim(),
            color = null,
            sortOrder = 0,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            version = 1
        )
        saveProject(project)
        return project
    }

    suspend fun softDeleteProject(projectId: String) {
        val now = TimeUtil.getCurrentIsoTime()
        todoProjectDao.softDeleteProject(projectId, now)
        todoDao.clearProject(projectId, now)
    }

    suspend fun saveTodo(todo: TodoEntity) {
        todoDao.insertTodo(todo)
    }

    suspend fun saveTodoWithHistory(todo: TodoEntity, action: String, summary: String, payloadJson: String = "{}") {
        todoDao.insertTodo(todo)
        addHistory(todo.id, action, summary, payloadJson)
    }

    suspend fun addHistory(todoId: String, action: String, summary: String, payloadJson: String = "{}") {
        todoHistoryDao.insertHistory(
            TodoHistoryEntity(
                id = TimeUtil.generateUuid(),
                todoId = todoId,
                action = action,
                summary = summary,
                payloadJson = payloadJson,
                createdAt = TimeUtil.getCurrentIsoTime()
            )
        )
    }

    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>> {
        return todoSubtaskDao.getSubtasksForTodo(todoId)
    }

    suspend fun getSubtasksSnapshotForTodo(todoId: String): List<TodoSubtaskEntity> {
        return todoSubtaskDao.getSubtasksSnapshotForTodo(todoId)
    }

    suspend fun saveSubtask(subtask: TodoSubtaskEntity) {
        todoSubtaskDao.insertSubtask(subtask)
        addHistory(subtask.todoId, "subtask_create", "添加子任务：${subtask.title}")
    }

    suspend fun updateSubtaskCompleted(id: String, isCompleted: Boolean, updatedAt: String) {
        todoSubtaskDao.updateCompleted(id, if (isCompleted) 1 else 0, updatedAt)
    }

    suspend fun deleteSubtask(id: String) {
        todoSubtaskDao.softDeleteSubtask(id, TimeUtil.getCurrentIsoTime())
    }

    suspend fun updateTodoStatus(id: String, status: String) {
        todoDao.updateTodoStatus(id, status)
    }

    suspend fun updatePriority(ids: Set<String>, priority: String) {
        if (ids.isEmpty()) return
        val now = TimeUtil.getCurrentIsoTime()
        todoDao.updatePriority(
            ids = ids.toList(),
            priority = priority,
            isImportant = if (priority == "high") 1 else 0,
            updatedAt = now
        )
        val summary = if (priority == "high") "标记为高优先级" else "标记为普通优先级"
        ids.forEach { id -> addHistory(id, "priority", summary) }
    }

    suspend fun softDeleteTodo(id: String) {
        val timestamp = TimeUtil.getCurrentIsoTime()
        todoDao.softDeleteTodo(id, timestamp)
        addHistory(id, "delete", "移入回收站")
    }
}
