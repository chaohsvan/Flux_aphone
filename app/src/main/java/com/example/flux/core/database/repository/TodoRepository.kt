package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.TodoDao
import com.example.flux.core.database.dao.TodoHistoryDao
import com.example.flux.core.database.dao.TodoProjectDao
import com.example.flux.core.database.dao.TodoSubtaskProgressEntity
import com.example.flux.core.database.dao.TodoSubtaskDao
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.reminder.ReminderScheduler
import com.example.flux.core.util.TimeUtil
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val todoSubtaskDao: TodoSubtaskDao,
    private val todoProjectDao: TodoProjectDao,
    private val todoHistoryDao: TodoHistoryDao,
    private val reminderScheduler: ReminderScheduler
) {
    fun getActiveTodos(): Flow<List<TodoEntity>> = todoDao.getActiveTodos()

    fun getTodosByDate(date: String): Flow<List<TodoEntity>> = todoDao.getTodosByDate(date)

    fun getDeletedTodos(): Flow<List<TodoEntity>> = todoDao.getDeletedTodos()

    fun getActiveProjects(): Flow<List<TodoProjectEntity>> = todoProjectDao.getActiveProjects()

    fun getHistoryForTodo(todoId: String): Flow<List<TodoHistoryEntity>> = todoHistoryDao.getHistoryForTodo(todoId)

    fun getSubtaskProgress(): Flow<List<TodoSubtaskProgressEntity>> = todoSubtaskDao.getSubtaskProgress()

    suspend fun getTodoById(id: String): TodoEntity? = todoDao.getTodoById(id)

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

    suspend fun renameProject(projectId: String, name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        todoProjectDao.renameProject(projectId, trimmedName, TimeUtil.getCurrentIsoTime())
    }

    suspend fun saveTodo(todo: TodoEntity) {
        todoDao.insertTodo(todo)
        reminderScheduler.scheduleTodo(todo)
    }

    suspend fun saveTodoWithHistory(todo: TodoEntity, action: String, summary: String, payloadJson: String = "{}") {
        todoDao.insertTodo(todo)
        reminderScheduler.scheduleTodo(todo)
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

    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>> = todoSubtaskDao.getSubtasksForTodo(todoId)

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

    suspend fun reorderSubtasks(todoId: String, orderedSubtasks: List<TodoSubtaskEntity>) {
        val now = TimeUtil.getCurrentIsoTime()
        orderedSubtasks.forEachIndexed { index, subtask ->
            if (subtask.sortOrder != index) {
                todoSubtaskDao.updateSortOrder(subtask.id, index, now)
            }
        }
        addHistory(todoId, "subtask_reorder", "调整子任务排序")
    }

    suspend fun deleteSubtask(id: String) {
        todoSubtaskDao.softDeleteSubtask(id, TimeUtil.getCurrentIsoTime())
    }

    suspend fun updateTodoStatus(id: String, status: String) {
        todoDao.updateTodoStatus(id, status)
        todoDao.getTodoById(id)?.let { reminderScheduler.scheduleTodo(it) }
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

    suspend fun reorderTodos(orderedTodos: List<TodoEntity>) {
        val now = TimeUtil.getCurrentIsoTime()
        orderedTodos.forEachIndexed { index, todo ->
            if (todo.sortOrder != index) {
                todoDao.updateSortOrder(todo.id, index, now)
            }
        }
    }

    suspend fun softDeleteTodo(id: String) {
        val timestamp = TimeUtil.getCurrentIsoTime()
        todoDao.softDeleteTodo(id, timestamp)
        reminderScheduler.cancelTodo(id)
        addHistory(id, "delete", "移入回收站")
    }

    suspend fun permanentlyDeleteTodo(id: String) {
        reminderScheduler.cancelTodo(id)
        todoSubtaskDao.permanentlyDeleteSubtasksForTodo(id)
        todoHistoryDao.permanentlyDeleteHistoryForTodo(id)
        todoDao.permanentlyDeleteTodo(id)
    }
}
