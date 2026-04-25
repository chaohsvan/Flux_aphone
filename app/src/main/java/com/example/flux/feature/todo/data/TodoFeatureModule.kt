package com.example.flux.feature.todo.data

import com.example.flux.feature.todo.domain.TodoFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TodoFeatureModule {

    @Binds
    abstract fun bindTodoFeatureGateway(
        gateway: DefaultTodoFeatureGateway
    ): TodoFeatureGateway
}
