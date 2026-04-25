package com.example.flux.feature.trash.domain

import android.content.Context
import com.example.flux.core.domain.diary.AttachmentDeleteResult
import com.example.flux.core.domain.diary.ManagedAttachment

interface AttachmentFeatureGateway {
    suspend fun scanAttachments(context: Context): List<ManagedAttachment>
    suspend fun cleanOrphans(context: Context): Long
    suspend fun deleteAttachment(attachment: ManagedAttachment, allowReferenced: Boolean): Long
    suspend fun deleteAttachments(
        context: Context,
        attachments: List<ManagedAttachment>,
        allowReferenced: Boolean
    ): AttachmentDeleteResult
}
