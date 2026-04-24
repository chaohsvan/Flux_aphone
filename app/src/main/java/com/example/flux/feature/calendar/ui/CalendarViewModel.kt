package com.example.flux.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.HolidayRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.calendar.CalendarAggregatorUseCase
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.domain.calendar.RestoreEventUseCase
import com.example.flux.core.domain.diary.RestoreDiaryUseCase
import com.example.flux.core.domain.todo.RestoreTodoUseCase
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarMonth(
    val year: Int,
    val month: Int
) {
    val label: String
        get() = String.format(Locale.CHINA, "%d\u5e74%d\u6708", year, month)

    fun next(): CalendarMonth {
        return if (month == 12) CalendarMonth(year + 1, 1) else copy(month = month + 1)
    }

    fun previous(): CalendarMonth {
        return if (month == 1) CalendarMonth(year - 1, 12) else copy(month = month - 1)
    }

    fun shift(offset: Int): CalendarMonth {
        if (offset == 0) return this
        var nextYear = year
        var nextMonth = month + offset
        while (nextMonth > 12) {
            nextMonth -= 12
            nextYear += 1
        }
        while (nextMonth < 1) {
            nextMonth += 12
            nextYear -= 1
        }
        return CalendarMonth(nextYear, nextMonth)
    }

    fun lengthOfMonth(): Int {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOffset(): Int {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun dateString(day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun isWeekend(day: Int): Boolean {
        val dayOfWeek = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    companion object {
        fun current(): CalendarMonth {
            val calendar = Calendar.getInstance()
            return CalendarMonth(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1
            )
        }
    }
}

data class CalendarDateDetails(
    val diary: DiaryEntity? = null,
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val isHoliday: Boolean = false,
    val holidayLabel: String? = null,
    val deletedDiaries: List<DiaryEntity> = emptyList(),
    val deletedTodos: List<TodoEntity> = emptyList(),
    val deletedEvents: List<CalendarEventEntity> = emptyList()
)

data class CalendarMonthHistory(
    val diaries: List<DiaryEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList()
)

data class HolidayOverrideState(
    val isHoliday: Boolean,
    val label: String?,
    val defaultIsHoliday: Boolean,
    val isUserOverride: Boolean
)

enum class CalendarSearchResultType {
    DIARY,
    TODO,
    EVENT
}

data class CalendarSearchResult(
    val id: String,
    val type: CalendarSearchResultType,
    val title: String,
    val subtitle: String,
    val date: String
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    calendarAggregatorUseCase: CalendarAggregatorUseCase,
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository,
    private val holidayRepository: HolidayRepository,
    private val restoreDiaryUseCase: RestoreDiaryUseCase,
    private val restoreTodoUseCase: RestoreTodoUseCase,
    private val restoreEventUseCase: RestoreEventUseCase,
    observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(CalendarMonth.current())
    val currentMonth: StateFlow<CalendarMonth> = _currentMonth.asStateFlow()

    val aggregatedData: StateFlow<Map<String, DailyAggregation>> = calendarAggregatorUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val projects: StateFlow<List<TodoProjectEntity>> = todoRepository.getActiveProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showDiaries = MutableStateFlow(false)
    val showDiaries = _showDiaries.asStateFlow()

    private val _showTodos = MutableStateFlow(false)
    val showTodos = _showTodos.asStateFlow()

    private val _showEvents = MutableStateFlow(true)
    val showEvents = _showEvents.asStateFlow()

    private val _showHolidays = MutableStateFlow(true)
    val showHolidays = _showHolidays.asStateFlow()

    private val _showTrash = MutableStateFlow(false)
    val showTrash = _showTrash.asStateFlow()

    private val _holidayEditMode = MutableStateFlow(false)
    val holidayEditMode = _holidayEditMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val trashSummary: StateFlow<TrashSummary> = observeTrashSummaryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

    val holidayOverrides: StateFlow<Map<String, HolidayOverrideState>> = combine(
        holidayRepository.getStaticHolidays(),
        holidayRepository.getHolidayOverrides()
    ) { staticHolidays, overrides ->
        val dates = (staticHolidays.map { it.date } + overrides.map { it.date }).toSet()
        dates.associateWith { date ->
            val static = staticHolidays.firstOrNull { it.date == date }
            val override = overrides.firstOrNull { it.date == date }
            val defaultIsHoliday = static?.let { it.isHoliday == 1 } ?: isWeekend(date)
            val isHoliday = override?.let { it.isHoliday == 1 } ?: defaultIsHoliday
            val label = when {
                override != null && isHoliday -> "\u624b\u52a8\u5047\u671f"
                override != null -> "\u624b\u52a8\u5de5\u4f5c\u65e5"
                static?.name?.isNotBlank() == true -> static.name
                defaultIsHoliday -> "\u5468\u672b\u5047\u671f"
                else -> null
            }
            HolidayOverrideState(
                isHoliday = isHoliday,
                label = label,
                defaultIsHoliday = defaultIsHoliday,
                isUserOverride = override != null
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    val monthHistory: StateFlow<CalendarMonthHistory> = combine(
        diaryRepository.getActiveDiaries(),
        todoRepository.getActiveTodos(),
        eventRepository.getActiveEvents(),
        _selectedDate
    ) { diaries, todos, events, date ->
        val selected = date ?: TimeUtil.getCurrentDate()
        val currentYear = selected.take(4)
        val month = selected.substringOrNull(5, 7)
        if (month == null) {
            CalendarMonthHistory()
        } else {
            CalendarMonthHistory(
                diaries = diaries.filter { it.entryDate.length >= 7 && it.entryDate.take(4) != currentYear && it.entryDate.substring(5, 7) == month },
                todos = todos.filter { todo ->
                    val due = todo.dueAt?.takeIf { it.length >= 7 }
                    due != null && due.take(4) != currentYear && due.substring(5, 7) == month
                },
                events = events.filter { event ->
                    event.startAt.length >= 7 && event.startAt.take(4) != currentYear && event.startAt.substring(5, 7) == month
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarMonthHistory()
    )

    val searchResults: StateFlow<List<CalendarSearchResult>> = combine(
        _searchQuery,
        diaryRepository.getActiveDiaries(),
        todoRepository.getActiveTodos(),
        eventRepository.getActiveEvents()
    ) { query, diaries, todos, events ->
        val keyword = query.trim()
        if (keyword.isBlank()) {
            emptyList()
        } else {
            buildList {
                diaries.asSequence()
                    .filter { diary ->
                        diary.matchesCalendarSearch(keyword, diary.title, diary.contentMd, diary.entryDate)
                    }
                    .mapTo(this) { diary ->
                        CalendarSearchResult(
                            id = diary.id,
                            type = CalendarSearchResultType.DIARY,
                            title = diary.title.ifBlank { "\u65e0\u6807\u9898\u65e5\u8bb0" },
                            subtitle = diary.contentMd.lineSequence().firstOrNull { it.isNotBlank() } ?: diary.entryDate,
                            date = diary.entryDate
                        )
                    }

                todos.asSequence()
                    .filter { todo ->
                        todo.matchesCalendarSearch(
                            keyword,
                            todo.title,
                            todo.description,
                            todo.startAt,
                            todo.dueAt,
                            todo.createdAt
                        )
                    }
                    .mapTo(this) { todo ->
                        CalendarSearchResult(
                            id = todo.id,
                            type = CalendarSearchResultType.TODO,
                            title = todo.title,
                            subtitle = listOfNotNull(todo.dueAt, todo.startAt, todo.createdAt).firstOrNull().orEmpty(),
                            date = todo.searchDate()
                        )
                    }

                events.asSequence()
                    .filter { event ->
                        event.matchesCalendarSearch(
                            keyword,
                            event.title,
                            event.description,
                            event.locationName,
                            event.startAt,
                            event.endAt
                        )
                    }
                    .mapTo(this) { event ->
                        CalendarSearchResult(
                            id = event.id,
                            type = CalendarSearchResultType.EVENT,
                            title = event.title,
                            subtitle = listOfNotNull(event.startAt, event.locationName).joinToString(" | "),
                            date = event.searchDate()
                        )
                    }
            }
                .sortedWith(compareByDescending<CalendarSearchResult> { it.date }.thenBy { it.title })
                .take(60)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateDetails: StateFlow<CalendarDateDetails?> = _selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(null)
            } else {
                val activeItems = combine(
                    diaryRepository.getDiaryFlowByDate(date),
                    todoRepository.getActiveTodos(),
                    eventRepository.getActiveEvents()
                ) { diary, todos, events ->
                    Triple(diary, todos, events)
                }
                val deletedItems = combine(
                    diaryRepository.getDeletedDiaries(),
                    todoRepository.getDeletedTodos(),
                    eventRepository.getDeletedEvents()
                ) { diaries, todos, events ->
                    Triple(diaries, todos, events)
                }
                combine(
                    activeItems,
                    deletedItems,
                    holidayOverrides
                ) { active, deleted, overrides ->
                    val (diary, todos, events) = active
                    val (deletedDiaries, deletedTodos, deletedEvents) = deleted
                    val override = overrides[date]
                    val defaultHoliday = override?.defaultIsHoliday ?: isWeekend(date)
                    val isHoliday = override?.isHoliday ?: defaultHoliday
                    CalendarDateDetails(
                        diary = diary,
                        todos = todos.filter { it.occursOn(date) },
                        events = events.filter { it.occursOn(date) },
                        isHoliday = isHoliday,
                        holidayLabel = override?.label ?: if (defaultHoliday) "\u5468\u672b\u5047\u671f" else null,
                        deletedDiaries = deletedDiaries.filter { it.entryDate == date },
                        deletedTodos = deletedTodos.filter { (it.dueAt ?: it.createdAt).take(10) == date },
                        deletedEvents = deletedEvents.filter { it.startAt.take(10) == date }
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleShowDiaries() {
        _showDiaries.update { !it }
    }

    fun toggleShowTodos() {
        _showTodos.update { !it }
    }

    fun toggleShowEvents() {
        _showEvents.update { !it }
    }

    fun toggleShowHolidays() {
        _showHolidays.update { !it }
    }

    fun toggleShowTrash() {
        _showTrash.update { !it }
    }

    fun toggleHolidayEditMode() {
        _holidayEditMode.update { !it }
    }

    fun selectDate(date: String?) {
        _selectedDate.value = date
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun focusDate(date: String) {
        val parts = date.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        if (year != null && month != null) {
            _currentMonth.value = CalendarMonth(year, month)
        }
        selectDate(date)
    }

    fun onDateClicked(date: String, defaultIsHoliday: Boolean) {
        if (_holidayEditMode.value) {
            viewModelScope.launch {
                holidayRepository.toggleHolidayOverride(date, defaultIsHoliday)
            }
        } else {
            selectDate(date)
        }
    }

    fun nextMonth() {
        _currentMonth.update { it.next() }
    }

    fun previousMonth() {
        _currentMonth.update { it.previous() }
    }

    fun shiftCurrentMonth(offset: Int) {
        _currentMonth.update { it.shift(offset) }
    }

    fun shiftSelectedDate(days: Int) {
        val baseDate = _selectedDate.value ?: _currentMonth.value.dateString(1)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = runCatching { formatter.parse(baseDate) }.getOrNull() ?: return
        val calendar = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_MONTH, days)
        }
        val nextDate = formatter.format(calendar.time)
        _selectedDate.value = nextDate
        _currentMonth.value = CalendarMonth(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1
        )
    }

    fun addEvent(input: CalendarEventInput) {
        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val event = CalendarEventEntity(
                id = TimeUtil.generateUuid(),
                title = input.title,
                description = input.description,
                startAt = input.startAt,
                endAt = input.endAt,
                allDay = if (input.allDay) 1 else 0,
                color = input.color,
                locationName = input.locationName,
                reminderMinutes = input.reminderMinutes,
                recurrenceRule = input.recurrenceRule,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            eventRepository.saveEvent(event)
        }
    }

    fun updateEvent(event: CalendarEventEntity, input: CalendarEventInput) {
        viewModelScope.launch {
            eventRepository.saveEvent(
                event.copy(
                    title = input.title,
                    description = input.description,
                    startAt = input.startAt,
                    endAt = input.endAt,
                    allDay = if (input.allDay) 1 else 0,
                    color = input.color,
                    locationName = input.locationName,
                    reminderMinutes = input.reminderMinutes,
                    recurrenceRule = input.recurrenceRule,
                    updatedAt = TimeUtil.getCurrentIsoTime(),
                    version = event.version + 1
                )
            )
        }
    }

    fun addProject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            todoRepository.createProject(trimmed)
        }
    }

    fun addTodo(title: String, description: String, priority: String, projectId: String?, dueAt: String) {
        if (!TimeUtil.isValidDateOrDateTime(dueAt.trim())) return
        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val todo = TodoEntity(
                id = TimeUtil.generateUuid(),
                projectId = projectId,
                title = title,
                description = description,
                status = "pending",
                priority = priority,
                dueAt = dueAt.trim().ifBlank { null },
                startAt = null,
                completedAt = null,
                sortOrder = 0,
                isImportant = if (priority == "high") 1 else 0,
                reminderMinutes = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 1
            )
            todoRepository.saveTodoWithHistory(todo, "create", "\u521b\u5efa\u5f85\u529e")
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            eventRepository.softDeleteEvent(id)
        }
    }

    fun restoreDiary(id: String) {
        viewModelScope.launch {
            restoreDiaryUseCase(id)
        }
    }

    fun restoreTodo(id: String) {
        viewModelScope.launch {
            restoreTodoUseCase(id)
        }
    }

    fun restoreEvent(id: String) {
        viewModelScope.launch {
            restoreEventUseCase(id)
        }
    }

    private fun isWeekend(date: String): Boolean {
        if (date.length < 10) return false
        val year = date.substring(0, 4).toIntOrNull() ?: return false
        val month = date.substring(5, 7).toIntOrNull() ?: return false
        val day = date.substring(8, 10).toIntOrNull() ?: return false
        val dayOfWeek = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? {
        return if (length >= endIndex) substring(startIndex, endIndex) else null
    }

    private fun TodoEntity.occursOn(date: String): Boolean {
        val raw = dueAt ?: createdAt
        return RecurrenceUtil.occurrenceDates(
            value = raw,
            recurrence = recurrence,
            interval = recurrenceInterval,
            until = recurrenceUntil,
            rangeStart = date,
            rangeEnd = date
        ).contains(date)
    }

    private fun CalendarEventEntity.occursOn(date: String): Boolean {
        return RecurrenceUtil.occurrenceDates(
            value = startAt,
            recurrence = recurrenceRule,
            rangeStart = date,
            rangeEnd = date
        ).contains(date)
    }

    private fun DiaryEntity.matchesCalendarSearch(query: String, vararg fields: String?): Boolean {
        return fields.any { field -> field.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun TodoEntity.matchesCalendarSearch(query: String, vararg fields: String?): Boolean {
        return fields.any { field -> field.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun CalendarEventEntity.matchesCalendarSearch(query: String, vararg fields: String?): Boolean {
        return fields.any { field -> field.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun TodoEntity.searchDate(): String {
        return dueAt?.takeIf { it.length >= 10 }?.take(10)
            ?: startAt?.takeIf { it.length >= 10 }?.take(10)
            ?: createdAt.take(10)
    }

    private fun CalendarEventEntity.searchDate(): String {
        return startAt.take(10)
    }
}
