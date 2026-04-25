package com.example.flux.feature.settings.data

import com.example.flux.feature.settings.domain.SettingsFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsFeatureModule {

    @Binds
    abstract fun bindSettingsFeatureGateway(
        gateway: DefaultSettingsFeatureGateway
    ): SettingsFeatureGateway
}
