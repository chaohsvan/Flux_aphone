package com.example.flux.core.domain.diary

import android.content.Context
import com.example.flux.core.database.dao.AttachmentMetadataDao
import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

data class ManagedAttachment(
    val file: File,
    val relativePath: String,
    val kind: AttachmentKind,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val sha256: String,
    val references: List<AttachmentReferenceSource>
) {
    val isReferenced: Boolean get() = references.isNotEmpty()
}

data class AttachmentDeleteResult(
    val deletedCount: Int,
    val freedSpaceBytes: Long,
    val skippedReferencedCount: Int,
    val deletedPaths: List<String>
)

data class AttachmentReferenceSource(
    val diaryId: String,
    val diaryDate: String,
    val diaryTime: String?,
    val lineNumber: Int,
    val snippet: String,
    val isDeleted: Boolean
)

enum class AttachmentKind {
    IMAGE,
    AUDIO,
    FILE
}

class AttachmentManagerUseCase @Inject constructor(
    private val diaryDao: DiaryDao,
    private val attachmentMetadataDao: AttachmentMetadataDao
) {
    suspend fun scanAttachments(context: Context): List<ManagedAttachment> {
        val attachmentsDir = DataPaths.attachmentsDir(context)
        if (!attachmentsDir.exists() || !attachmentsDir.isDirectory) {
            attachmentMetadataDao.deleteAll()
            return emptyList()
        }

        val allLocalFiles = attachmentsDir.walkTopDown()
            .filter { it.isFile }
            .toList()
        if (allLocalFiles.isEmpty()) {
            attachmentMetadataDao.deleteAll()
            return emptyList()
        }

        val existingMetadata = attachmentMetadataDao.getByPaths(
            allLocalFiles.map { file -> DataPaths.relativeToDataDir(context, file) }
        ).associateBy { it.relativePath }

        val allDiaries = diaryDao.getAllDiariesSnapshot()
        val attachmentRegex = Regex("/?(?:assets/)?attachments/[^)\\s]+")
        val referencesByPath = mutableMapOf<String, MutableList<AttachmentReferenceSource>>()

        allDiaries.forEach { diary ->
            diary.contentMd.lines().forEachIndexed { index, line ->
                attachmentRegex.findAll(line).forEach { match ->
                    val path = normalizeAttachmentPath(match.value)
                    val source = AttachmentReferenceSource(
                        diaryId = diary.id,
                        diaryDate = diary.entryDate,
                        diaryTime = diary.entryTime,
                        lineNumber = index + 1,
                        snippet = line.trim().take(120),
                        isDeleted = diary.deletedAt != null
                    )
                    referencesByPath.getOrPut(path) { mutableListOf() }.add(source)
                }
            }
        }

        val now = TimeUtil.getCurrentIsoTime()
        val metadata = mutableListOf<AttachmentMetadataEntity>()
        val attachments = allLocalFiles.map { file ->
            val relativePath = DataPaths.relativeToDataDir(context, file)
            val kind = file.attachmentKind()
            val sizeBytes = file.length()
            val modifiedAt = file.lastModified()
            val cached = existingMetadata[relativePath]
            val sha256 = if (
                cached != null &&
                cached.sizeBytes == sizeBytes &&
                cached.modifiedAt == modifiedAt
            ) {
                cached.sha256
            } else {
                file.sha256()
            }
            val references = referencesByPath[relativePath].orEmpty()
            metadata += AttachmentMetadataEntity(
                relativePath = relativePath,
                fileName = file.name,
                kind = kind.name,
                sizeBytes = sizeBytes,
                modifiedAt = modifiedAt,
                sha256 = sha256,
                referenceCount = references.size,
                lastScannedAt = now
            )
            ManagedAttachment(
                file = file,
                relativePath = relativePath,
                kind = kind,
                sizeBytes = sizeBytes,
                modifiedAt = modifiedAt,
                sha256 = sha256,
                references = references
            )
        }
        attachmentMetadataDao.upsertAll(metadata)
        attachmentMetadataDao.deleteNotIn(attachments.map { it.relativePath })
        return attachments
    }

    suspend fun cleanOrphans(context: Context): Long {
        val orphans = scanAttachments(context).filterNot { it.isReferenced }
        return deleteAttachments(context, orphans, allowReferenced = false).freedSpaceBytes
    }

    suspend fun deleteUnreferenced(context: Context, attachment: ManagedAttachment): Long {
        return deleteAttachment(attachment, allowReferenced = false)
    }

    suspend fun deleteAttachment(attachment: ManagedAttachment, allowReferenced: Boolean): Long {
        if (attachment.isReferenced && !allowReferenced) return 0L
        val size = attachment.file.length()
        return if (attachment.file.delete()) {
            attachmentMetadataDao.deleteByPath(attachment.relativePath)
            size
        } else {
            0L
        }
    }

    suspend fun deleteAttachments(
        context: Context,
        attachments: List<ManagedAttachment>,
        allowReferenced: Boolean
    ): AttachmentDeleteResult {
        var freedSpace = 0L
        var deletedCount = 0
        var skippedReferencedCount = 0
        val deletedPaths = mutableListOf<String>()
        for (attachment in attachments) {
            if (attachment.isReferenced && !allowReferenced) {
                skippedReferencedCount += 1
                continue
            }
            val size = attachment.file.length()
            if (attachment.file.delete()) {
                deletedCount += 1
                freedSpace += size
                deletedPaths += attachment.relativePath
            }
        }
        if (deletedPaths.isNotEmpty()) {
            attachmentMetadataDao.deleteByPaths(deletedPaths)
        }
        cleanEmptyDirectories(DataPaths.attachmentsDir(context))
        return AttachmentDeleteResult(
            deletedCount = deletedCount,
            freedSpaceBytes = freedSpace,
            skippedReferencedCount = skippedReferencedCount,
            deletedPaths = deletedPaths
        )
    }

    private fun cleanEmptyDirectories(root: File) {
        if (!root.exists() || !root.isDirectory) return
        root.walkBottomUp()
            .filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
            .forEach { it.delete() }
    }

    private fun normalizeAttachmentPath(path: String): String {
        return DataPaths.normalizeAttachmentPath(path)
    }

    private fun File.attachmentKind(): AttachmentKind {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> AttachmentKind.IMAGE
            "mp3", "wav", "m4a", "aac", "ogg", "flac" -> AttachmentKind.AUDIO
            else -> AttachmentKind.FILE
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
