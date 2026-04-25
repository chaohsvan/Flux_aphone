package com.example.flux.feature.calendar.data

import com.example.flux.feature.calendar.domain.CalendarFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarFeatureModule {

    @Binds
    abstract fun bindCalendarFeatureGateway(
        gateway: DefaultCalendarFeatureGateway
    ): CalendarFeatureGateway
}
