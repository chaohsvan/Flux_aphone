package com.example.flux.feature.trash.data

import com.example.flux.feature.trash.domain.AttachmentFeatureGateway
import com.example.flux.feature.trash.domain.TrashFeatureGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TrashFeatureModule {

    @Binds
    abstract fun bindAttachmentFeatureGateway(
        gateway: DefaultAttachmentFeatureGateway
    ): AttachmentFeatureGateway

    @Binds
    abstract fun bindTrashFeatureGateway(
        gateway: DefaultTrashFeatureGateway
    ): TrashFeatureGateway
}
