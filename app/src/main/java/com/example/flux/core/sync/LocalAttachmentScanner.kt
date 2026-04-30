package com.example.flux.core.sync

import android.content.Context
import com.example.flux.core.database.dao.AttachmentMetadataDao
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAttachmentScanner @Inject constructor(
    private val attachmentMetadataDao: AttachmentMetadataDao
) {
    suspend fun scan(context: Context, deviceId: String): List<AttachmentSyncEntry> {
        val attachmentsDir = DataPaths.attachmentsDir(context)
        if (!attachmentsDir.exists() || !attachmentsDir.isDirectory) return emptyList()
        val files = attachmentsDir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return emptyList()

        val paths = files.map { DataPaths.relativeToDataDir(context, it) }
        val cachedByPath = attachmentMetadataDao.getByPaths(paths).associateBy { it.relativePath }
        val now = TimeUtil.getCurrentIsoTime()
        val metadata = mutableListOf<AttachmentMetadataEntity>()
        val entries = files
            .map { file ->
                val relativePath = DataPaths.relativeToDataDir(context, file)
                val cached = cachedByPath[relativePath]
                val sha256 = if (
                    cached != null &&
                    cached.sizeBytes == file.length() &&
                    cached.modifiedAt == file.lastModified()
                ) {
                    cached.sha256
                } else {
                    HashUtil.sha256(file)
                }
                metadata += AttachmentMetadataEntity(
                    relativePath = relativePath,
                    fileName = file.name,
                    kind = file.attachmentKind(),
                    sizeBytes = file.length(),
                    modifiedAt = file.lastModified(),
                    sha256 = sha256,
                    referenceCount = cached?.referenceCount ?: 0,
                    lastScannedAt = now
                )
                AttachmentSyncEntry(
                    path = relativePath,
                    sha256 = sha256,
                    sizeBytes = file.length(),
                    modifiedAt = file.lastModified(),
                    updatedAt = now,
                    updatedByDeviceId = deviceId
                )
            }
            .sortedBy { it.path }
            .toList()
        attachmentMetadataDao.upsertAll(metadata)
        return entries
    }

    fun fileForPath(context: Context, relativePath: String): File {
        return DataPaths.attachmentFile(context, relativePath).canonicalFile
    }

    private fun File.attachmentKind(): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "IMAGE"
            "mp3", "wav", "m4a", "aac", "ogg", "flac" -> "AUDIO"
            else -> "FILE"
        }
    }
}
