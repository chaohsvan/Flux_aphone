package com.example.flux.feature.trash.domain

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

interface TrashFeatureGateway {
    fun getDeletedDiaries(): Flow<List<DiaryEntity>>
    fun getDeletedTodos(): Flow<List<TodoEntity>>
    fun getDeletedEvents(): Flow<List<CalendarEventEntity>>

    suspend fun restoreDiary(id: String)
    suspend fun restoreTodo(id: String)
    suspend fun restoreEvent(id: String)
}
