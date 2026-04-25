package com.example.flux.feature.search.data

import com.example.flux.feature.search.domain.SearchFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchFeatureModule {

    @Binds
    abstract fun bindSearchFeatureGateway(
        gateway: DefaultSearchFeatureGateway
    ): SearchFeatureGateway
}
