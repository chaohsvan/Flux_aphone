package com.example.flux.core.util

import android.content.Context
import java.io.File

object DataPaths {
    const val DATA_DIR_NAME = "data"
    const val DATABASE_NAME = "flux.db"
    const val ATTACHMENTS_DIR_NAME = "attachments"
    private const val LEGACY_ATTACHMENTS_PATH = "assets/attachments"

    fun dataDir(context: Context): File {
        return File(context.filesDir, DATA_DIR_NAME)
    }

    fun ensureDataDir(context: Context): File {
        return dataDir(context).apply { mkdirs() }
    }

    fun databaseFile(context: Context): File {
        return File(ensureDataDir(context), DATABASE_NAME)
    }

    fun attachmentsDir(context: Context): File {
        return File(ensureDataDir(context), ATTACHMENTS_DIR_NAME)
    }

    fun legacyAttachmentsDir(context: Context): File {
        return File(context.filesDir, LEGACY_ATTACHMENTS_PATH)
    }

    fun normalizeAttachmentPath(path: String): String {
        val cleanPath = path.trim().removePrefix("/")
        return when {
            cleanPath == LEGACY_ATTACHMENTS_PATH -> ATTACHMENTS_DIR_NAME
            cleanPath.startsWith("$LEGACY_ATTACHMENTS_PATH/") -> cleanPath.removePrefix("assets/")
            else -> cleanPath
        }
    }

    fun attachmentFile(context: Context, path: String): File {
        return File(ensureDataDir(context), normalizeAttachmentPath(path))
    }

    fun relativeToDataDir(context: Context, file: File): String {
        return file.relativeTo(ensureDataDir(context)).invariantSeparatorsPath
    }
}
