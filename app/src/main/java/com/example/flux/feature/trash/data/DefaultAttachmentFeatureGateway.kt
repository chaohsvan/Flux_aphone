package com.example.flux.feature.trash.data

import android.content.Context
import com.example.flux.core.domain.diary.AttachmentDeleteResult
import com.example.flux.core.domain.diary.AttachmentManagerUseCase
import com.example.flux.core.domain.diary.ManagedAttachment
import com.example.flux.feature.trash.domain.AttachmentFeatureGateway
import javax.inject.Inject

class DefaultAttachmentFeatureGateway @Inject constructor(
    private val attachmentManagerUseCase: AttachmentManagerUseCase
) : AttachmentFeatureGateway {

    override suspend fun scanAttachments(context: Context): List<ManagedAttachment> {
        return attachmentManagerUseCase.scanAttachments(context)
    }

    override suspend fun cleanOrphans(context: Context): Long {
        return attachmentManagerUseCase.cleanOrphans(context)
    }

    override suspend fun deleteAttachment(attachment: ManagedAttachment, allowReferenced: Boolean): Long {
        return attachmentManagerUseCase.deleteAttachment(attachment, allowReferenced)
    }

    override suspend fun deleteAttachments(
        context: Context,
        attachments: List<ManagedAttachment>,
        allowReferenced: Boolean
    ): AttachmentDeleteResult {
        return attachmentManagerUseCase.deleteAttachments(context, attachments, allowReferenced)
    }
}
