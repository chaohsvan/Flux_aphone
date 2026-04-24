package com.example.flux.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.dao.AttachmentMetadataDao
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class GlobalSearchScope(val label: String) {
    ALL("\u5168\u90e8"),
    DIARY("\u65e5\u8bb0"),
    TODO("\u5f85\u529e"),
    EVENT("\u4e8b\u4ef6"),
    ATTACHMENT("\u9644\u4ef6")
}

enum class GlobalSearchResultType(val label: String) {
    DIARY("\u65e5\u8bb0"),
    TODO("\u5f85\u529e"),
    EVENT("\u4e8b\u4ef6"),
    ATTACHMENT("\u9644\u4ef6")
}

data class GlobalSearchResult(
    val id: String,
    val type: GlobalSearchResultType,
    val title: String,
    val subtitle: String,
    val overline: String,
    val dateSortKey: String = "",
    val attachmentQuery: String? = null
)

private data class GlobalSearchSources(
    val diaries: List<DiaryEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val attachments: List<AttachmentMetadataEntity> = emptyList()
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    diaryRepository: DiaryRepository,
    todoRepository: TodoRepository,
    eventRepository: EventRepository,
    attachmentMetadataDao: AttachmentMetadataDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _scope = MutableStateFlow(GlobalSearchScope.ALL)
    val scope: StateFlow<GlobalSearchScope> = _scope.asStateFlow()

    private val sources = combine(
        diaryRepository.getActiveDiaries(),
        todoRepository.getActiveTodos(),
        eventRepository.getActiveEvents(),
        attachmentMetadataDao.observeAll()
    ) { diaries, todos, events, attachments ->
        GlobalSearchSources(
            diaries = diaries,
            todos = todos,
            events = events,
            attachments = attachments
        )
    }

    val results: StateFlow<List<GlobalSearchResult>> = combine(
        _query,
        _scope,
        sources
    ) { query, scope, source ->
        val keyword = query.trim()
        if (keyword.isBlank()) {
            emptyList()
        } else {
            buildList {
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.DIARY) {
                    source.diaries.asSequence()
                        .filter { it.matches(keyword) }
                        .mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.TODO) {
                    source.todos.asSequence()
                        .filter { it.matches(keyword) }
                        .mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.EVENT) {
                    source.events.asSequence()
                        .filter { it.matches(keyword) }
                        .mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.ATTACHMENT) {
                    source.attachments.asSequence()
                        .filter { it.matches(keyword) }
                        .mapTo(this) { it.toSearchResult() }
                }
            }
                .sortedWith(
                    compareByDescending<GlobalSearchResult> { it.dateSortKey }
                        .thenBy { it.type.ordinal }
                        .thenBy { it.title.lowercase() }
                )
                .take(80)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun updateScope(value: GlobalSearchScope) {
        _scope.value = value
    }

    private fun DiaryEntity.matches(query: String): Boolean {
        return listOf(title, contentMd, entryDate, entryTime, mood, weather, locationName)
            .any { it.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun TodoEntity.matches(query: String): Boolean {
        return listOf(title, description, dueAt, startAt, createdAt)
            .any { it.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun CalendarEventEntity.matches(query: String): Boolean {
        return listOf(title, description, locationName, startAt, endAt)
            .any { it.orEmpty().contains(query, ignoreCase = true) }
    }

    private fun AttachmentMetadataEntity.matches(query: String): Boolean {
        return listOf(fileName, relativePath, sha256, kind)
            .any { it.contains(query, ignoreCase = true) }
    }

    private fun DiaryEntity.toSearchResult(): GlobalSearchResult {
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.DIARY,
            title = title.ifBlank { "\u65e0\u6807\u9898\u65e5\u8bb0" },
            subtitle = contentMd.lineSequence().firstOrNull { it.isNotBlank() } ?: entryDate,
            overline = "\u65e5\u8bb0 | $entryDate",
            dateSortKey = entryDate
        )
    }

    private fun TodoEntity.toSearchResult(): GlobalSearchResult {
        val time = dueAt?.takeIf { it.isNotBlank() } ?: startAt?.takeIf { it.isNotBlank() } ?: createdAt
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.TODO,
            title = title,
            subtitle = description.ifBlank { time },
            overline = "\u5f85\u529e | ${time.take(10)}",
            dateSortKey = time.take(10)
        )
    }

    private fun CalendarEventEntity.toSearchResult(): GlobalSearchResult {
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.EVENT,
            title = title.ifBlank { "\u672a\u547d\u540d\u4e8b\u4ef6" },
            subtitle = listOfNotNull(
                locationName?.takeIf { it.isNotBlank() },
                description.takeIf { it.isNotBlank() }
            ).joinToString(" | ").ifBlank { startAt },
            overline = "\u4e8b\u4ef6 | ${startAt.take(10)}",
            dateSortKey = startAt.take(10)
        )
    }

    private fun AttachmentMetadataEntity.toSearchResult(): GlobalSearchResult {
        val referenceLabel = if (referenceCount > 0) {
            "\u5df2\u5f15\u7528 $referenceCount"
        } else {
            "\u672a\u5f15\u7528"
        }
        return GlobalSearchResult(
            id = relativePath,
            type = GlobalSearchResultType.ATTACHMENT,
            title = fileName,
            subtitle = relativePath,
            overline = "\u9644\u4ef6 | $referenceLabel",
            dateSortKey = modifiedAt.toString(),
            attachmentQuery = relativePath
        )
    }
}
