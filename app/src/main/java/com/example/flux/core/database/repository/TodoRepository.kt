package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.TodoDao
import com.example.flux.core.database.dao.TodoSubtaskDao
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val todoSubtaskDao: TodoSubtaskDao
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

    suspend fun getTodoById(id: String): TodoEntity? {
        return todoDao.getTodoById(id)
    }

    suspend fun saveTodo(todo: TodoEntity) {
        todoDao.insertTodo(todo)
    }

    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>> {
        return todoSubtaskDao.getSubtasksForTodo(todoId)
    }

    suspend fun getSubtasksSnapshotForTodo(todoId: String): List<TodoSubtaskEntity> {
        return todoSubtaskDao.getSubtasksSnapshotForTodo(todoId)
    }

    suspend fun saveSubtask(subtask: TodoSubtaskEntity) {
        todoSubtaskDao.insertSubtask(subtask)
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
        todoDao.updatePriority(
            ids = ids.toList(),
            priority = priority,
            isImportant = if (priority == "high") 1 else 0,
            updatedAt = TimeUtil.getCurrentIsoTime()
        )
    }

    suspend fun softDeleteTodo(id: String) {
        val timestamp = TimeUtil.getCurrentIsoTime()
        todoDao.softDeleteTodo(id, timestamp)
    }
}
