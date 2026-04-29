package com.example.flux.feature.calendar.domain

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.database.entity.CalendarStaticHolidayEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import kotlinx.coroutines.flow.Flow

interface CalendarFeatureGateway {
    fun getActiveProjects(): Flow<List<TodoProjectEntity>>
    fun getStaticHolidays(): Flow<List<CalendarStaticHolidayEntity>>
    fun getHolidayOverrides(): Flow<List<CalendarHolidayOverrideEntity>>
    fun getActiveDiaries(): Flow<List<DiaryEntity>>
    fun getDiaryFlowByDate(date: String): Flow<DiaryEntity?>
    fun getDeletedDiaries(): Flow<List<DiaryEntity>>
    fun getActiveTodos(): Flow<List<TodoEntity>>
    fun getDeletedTodos(): Flow<List<TodoEntity>>
    fun getActiveEvents(): Flow<List<CalendarEventEntity>>
    fun getDeletedEvents(): Flow<List<CalendarEventEntity>>
    suspend fun saveEvent(event: CalendarEventEntity)
    suspend fun softDeleteEvent(id: String)
    suspend fun softDeleteEventFromDate(id: String, date: String)
    suspend fun toggleHolidayOverride(date: String, defaultIsHoliday: Boolean)
    suspend fun createProject(name: String): TodoProjectEntity
    suspend fun renameProject(projectId: String, name: String)
    suspend fun softDeleteProject(projectId: String)
    suspend fun saveTodoWithHistory(
        todo: TodoEntity,
        action: String,
        summary: String,
        payloadJson: String = "{}"
    )
}
