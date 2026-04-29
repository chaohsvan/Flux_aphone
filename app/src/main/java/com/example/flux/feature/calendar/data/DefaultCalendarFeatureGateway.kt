package com.example.flux.feature.calendar.data

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.database.entity.CalendarStaticHolidayEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.HolidayRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.feature.calendar.domain.CalendarFeatureGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultCalendarFeatureGateway @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository,
    private val holidayRepository: HolidayRepository
) : CalendarFeatureGateway {

    override fun getActiveProjects(): Flow<List<TodoProjectEntity>> = todoRepository.getActiveProjects()

    override fun getStaticHolidays(): Flow<List<CalendarStaticHolidayEntity>> = holidayRepository.getStaticHolidays()

    override fun getHolidayOverrides(): Flow<List<CalendarHolidayOverrideEntity>> = holidayRepository.getHolidayOverrides()

    override fun getActiveDiaries(): Flow<List<DiaryEntity>> = diaryRepository.getActiveDiaries()

    override fun getDiaryFlowByDate(date: String): Flow<DiaryEntity?> = diaryRepository.getDiaryFlowByDate(date)

    override fun getDeletedDiaries(): Flow<List<DiaryEntity>> = diaryRepository.getDeletedDiaries()

    override fun getActiveTodos(): Flow<List<TodoEntity>> = todoRepository.getActiveTodos()

    override fun getDeletedTodos(): Flow<List<TodoEntity>> = todoRepository.getDeletedTodos()

    override fun getActiveEvents(): Flow<List<CalendarEventEntity>> = eventRepository.getActiveEvents()

    override fun getDeletedEvents(): Flow<List<CalendarEventEntity>> = eventRepository.getDeletedEvents()

    override suspend fun saveEvent(event: CalendarEventEntity) {
        eventRepository.saveEvent(event)
    }

    override suspend fun softDeleteEvent(id: String) {
        eventRepository.softDeleteEvent(id)
    }

    override suspend fun softDeleteEventFromDate(id: String, date: String) {
        eventRepository.softDeleteEventFromDate(id, date)
    }

    override suspend fun toggleHolidayOverride(date: String, defaultIsHoliday: Boolean) {
        holidayRepository.toggleHolidayOverride(date, defaultIsHoliday)
    }

    override suspend fun createProject(name: String): TodoProjectEntity = todoRepository.createProject(name)

    override suspend fun renameProject(projectId: String, name: String) {
        todoRepository.renameProject(projectId, name)
    }

    override suspend fun softDeleteProject(projectId: String) {
        todoRepository.softDeleteProject(projectId)
    }

    override suspend fun saveTodoWithHistory(
        todo: TodoEntity,
        action: String,
        summary: String,
        payloadJson: String
    ) {
        todoRepository.saveTodoWithHistory(todo, action, summary, payloadJson)
    }
}
