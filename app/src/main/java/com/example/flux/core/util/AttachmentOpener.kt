package com.example.flux.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object AttachmentOpener {
    fun open(context: Context, path: String): Boolean {
        val file = resolveAttachmentFile(context, path) ?: return false
        return open(context, file)
    }

    fun open(context: Context, file: File): Boolean {
        if (!file.exists() || !file.isFile) return false

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun resolveAttachmentFile(context: Context, path: String): File? {
        val normalized = DataPaths.normalizeAttachmentPath(path)
        val dataFile = DataPaths.attachmentFile(context, normalized)
        if (dataFile.exists() && dataFile.isFile) return dataFile

        val legacyFile = File(context.filesDir, "assets/$normalized")
        if (legacyFile.exists() && legacyFile.isFile) return legacyFile

        return null
    }

    private fun mimeType(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "md" -> "text/markdown"
                "csv" -> "text/csv"
                "json" -> "application/json"
                "zip" -> "application/zip"
                "rar" -> "application/vnd.rar"
                "7z" -> "application/x-7z-compressed"
                else -> "*/*"
            }
    }
}
