package com.example.flux.feature.search.data

import com.example.flux.core.database.dao.AttachmentMetadataDao
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.database.repository.EventRepository
import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.feature.search.domain.SearchFeatureGateway
import com.example.flux.feature.search.domain.SearchSourceBundle
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DefaultSearchFeatureGateway @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val eventRepository: EventRepository,
    private val attachmentMetadataDao: AttachmentMetadataDao
) : SearchFeatureGateway {

    override fun observeSources(): Flow<SearchSourceBundle> {
        return combine(
            diaryRepository.getActiveDiaries(),
            todoRepository.getActiveTodos(),
            eventRepository.getActiveEvents(),
            attachmentMetadataDao.observeAll()
        ) { diaries, todos, events, attachments ->
            SearchSourceBundle(
                diaries = diaries,
                todos = todos,
                events = events,
                attachments = attachments
            )
        }
    }
}
