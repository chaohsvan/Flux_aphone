package com.example.flux.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.search.domain.SearchFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class GlobalSearchScope(val label: String) {
    ALL("全部"),
    DIARY("日记"),
    TODO("待办"),
    EVENT("事件"),
    ATTACHMENT("附件")
}

enum class GlobalSearchResultType(val label: String) {
    DIARY("日记"),
    TODO("待办"),
    EVENT("事件"),
    ATTACHMENT("附件")
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

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    searchFeatureGateway: SearchFeatureGateway
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _scope = MutableStateFlow(GlobalSearchScope.ALL)
    val scope: StateFlow<GlobalSearchScope> = _scope.asStateFlow()

    private val sources = searchFeatureGateway.observeSources()

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
                    source.diaries.asSequence().filter { it.matches(keyword) }.mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.TODO) {
                    source.todos.asSequence().filter { it.matches(keyword) }.mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.EVENT) {
                    source.events.asSequence().filter { it.matches(keyword) }.mapTo(this) { it.toSearchResult() }
                }
                if (scope == GlobalSearchScope.ALL || scope == GlobalSearchScope.ATTACHMENT) {
                    source.attachments.asSequence().filter { it.matches(keyword) }.mapTo(this) { it.toSearchResult() }
                }
            }.sortedWith(
                compareByDescending<GlobalSearchResult> { it.dateSortKey }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.title.lowercase() }
            ).take(80)
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
        return listOf(fileName, relativePath, sha256, kind).any { it.contains(query, ignoreCase = true) }
    }

    private fun DiaryEntity.toSearchResult(): GlobalSearchResult {
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.DIARY,
            title = title.ifBlank { "无标题日记" },
            subtitle = contentMd.lineSequence().firstOrNull { it.isNotBlank() } ?: entryDate,
            overline = "日记 | $entryDate",
            dateSortKey = entryDate
        )
    }

    private fun TodoEntity.toSearchResult(): GlobalSearchResult {
        val time = dueAt?.takeIf { it.isNotBlank() } ?: startAt?.takeIf { it.isNotBlank() }
        val fallbackTime = TimeUtil.formatTimestampForDisplay(createdAt)
        val dateSortKey = TimeUtil.localDatePart(time ?: createdAt).orEmpty()
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.TODO,
            title = title,
            subtitle = description.ifBlank { time ?: fallbackTime },
            overline = "待办 | $dateSortKey",
            dateSortKey = dateSortKey
        )
    }

    private fun CalendarEventEntity.toSearchResult(): GlobalSearchResult {
        return GlobalSearchResult(
            id = id,
            type = GlobalSearchResultType.EVENT,
            title = title.ifBlank { "未命名事件" },
            subtitle = listOfNotNull(
                locationName?.takeIf { it.isNotBlank() },
                description.takeIf { it.isNotBlank() }
            ).joinToString(" | ").ifBlank { startAt },
            overline = "事件 | ${startAt.take(10)}",
            dateSortKey = startAt.take(10)
        )
    }

    private fun AttachmentMetadataEntity.toSearchResult(): GlobalSearchResult {
        val referenceLabel = if (referenceCount > 0) {
            "已引用 $referenceCount"
        } else {
            "未引用"
        }
        return GlobalSearchResult(
            id = relativePath,
            type = GlobalSearchResultType.ATTACHMENT,
            title = fileName,
            subtitle = relativePath,
            overline = "附件 | $referenceLabel",
            dateSortKey = modifiedAt.toString(),
            attachmentQuery = relativePath
        )
    }
}
