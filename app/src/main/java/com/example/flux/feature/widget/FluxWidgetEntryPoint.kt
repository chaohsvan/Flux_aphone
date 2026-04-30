package com.example.flux.feature.widget

import com.example.flux.core.database.repository.TodoRepository
import com.example.flux.core.domain.calendar.CalendarAggregatorUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FluxWidgetEntryPoint {
    fun todoRepository(): TodoRepository
    fun calendarAggregatorUseCase(): CalendarAggregatorUseCase
}
