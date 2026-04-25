package com.example.flux.feature.search.domain

import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

data class SearchSourceBundle(
    val diaries: List<DiaryEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val attachments: List<AttachmentMetadataEntity> = emptyList()
)

interface SearchFeatureGateway {
    fun observeSources(): Flow<SearchSourceBundle>
}
