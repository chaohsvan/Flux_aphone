package com.example.flux.core.domain.calendar

import com.example.flux.core.database.repository.EventRepository
import javax.inject.Inject

class RestoreEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(id: String) {
        eventRepository.restoreEvent(id)
    }
}
