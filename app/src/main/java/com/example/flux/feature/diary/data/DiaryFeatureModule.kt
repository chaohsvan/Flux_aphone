package com.example.flux.feature.diary.data

import com.example.flux.feature.diary.domain.DiaryFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DiaryFeatureModule {

    @Binds
    abstract fun bindDiaryFeatureGateway(
        gateway: DefaultDiaryFeatureGateway
    ): DiaryFeatureGateway
}
