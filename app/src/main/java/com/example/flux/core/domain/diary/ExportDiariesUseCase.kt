package com.example.flux.core.domain.diary

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.util.DataPaths
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class DiaryExportFormat(
    val extension: String,
    val mimeType: String
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    MARKDOWN("md", "text/markdown"),
    ZIP("zip", "application/zip")
}

class ExportDiariesUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(
        context: Context,
        ids: Set<String>,
        destUri: Uri,
        format: DiaryExportFormat
    ) {
        if (ids.isEmpty()) return

        if (format == DiaryExportFormat.ZIP) {
            exportZip(context, ids, destUri)
            return
        }

        val records = ids.mapNotNull { id ->
            val diary = diaryRepository.getDiaryById(id) ?: return@mapNotNull null
            DiaryExportRecord(diary, diaryRepository.getTagsForDiary(id))
        }
        val content = when (format) {
            DiaryExportFormat.JSON -> records.toJson()
            DiaryExportFormat.CSV -> records.toCsv()
            DiaryExportFormat.MARKDOWN -> records.toMarkdown()
            DiaryExportFormat.ZIP -> ""
        }

        context.contentResolver.openOutputStream(destUri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    private suspend fun exportZip(context: Context, ids: Set<String>, destUri: Uri) {
        val outStream: OutputStream? = context.contentResolver.openOutputStream(destUri)
        if (outStream == null) return

        ZipOutputStream(outStream).use { zos ->
            val writtenEntries = mutableSetOf<String>()
            for (id in ids) {
                val diary = diaryRepository.getDiaryById(id) ?: continue
                
                val safeTitle = diary.title.ifBlank { "无标题_${diary.entryDate}" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val mdFileName = "${diary.entryDate}_$safeTitle.md"
                if (!writtenEntries.add(mdFileName)) continue
                
                zos.putNextEntry(ZipEntry(mdFileName))
                zos.write(diary.contentMd.toByteArray())
                zos.closeEntry()

                val attachmentRegex = Regex("\\]\\((/?(?:assets/)?attachments/[^)]+)\\)")
                val matches = attachmentRegex.findAll(diary.contentMd)
                
                for (match in matches) {
                    val relativePath = normalizeAttachmentPath(match.groupValues[1])
                    val localFile = DataPaths.attachmentFile(context, relativePath)
                    val zipEntryName = relativePath
                    if (!writtenEntries.add(zipEntryName)) continue
                    
                    if (localFile.exists()) {
                        zos.putNextEntry(ZipEntry(zipEntryName))
                        FileInputStream(localFile).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    } else {
                        val assetPath = relativePath.removePrefix("/")
                        runCatching {
                            context.assets.open(assetPath).use { input ->
                                zos.putNextEntry(ZipEntry(zipEntryName))
                                input.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun normalizeAttachmentPath(path: String): String {
        return DataPaths.normalizeAttachmentPath(path)
    }
}

private data class DiaryExportRecord(
    val diary: DiaryEntity,
    val tags: List<DiaryTagEntity>
)

private fun List<DiaryExportRecord>.toJson(): String {
    val root = JSONArray()
    forEach { record ->
        val diary = record.diary
        root.put(
            JSONObject()
                .put("id", diary.id)
                .put("entry_date", diary.entryDate)
                .put("entry_time", diary.entryTime)
                .put("title", diary.title)
                .put("content_md", diary.contentMd)
                .put("mood", diary.mood)
                .put("weather", diary.weather)
                .put("location_name", diary.locationName)
                .put("is_favorite", diary.isFavorite == 1)
                .put("word_count", diary.wordCount)
                .put("created_at", diary.createdAt)
                .put("updated_at", diary.updatedAt)
                .put("tags", JSONArray(record.tags.map { it.name }))
        )
    }
    return root.toString(2)
}

private fun List<DiaryExportRecord>.toCsv(): String {
    val rows = mutableListOf(
        listOf("entry_date", "entry_time", "title", "mood", "weather", "location_name", "is_favorite", "tags", "content_md")
    )
    forEach { record ->
        val diary = record.diary
        rows += listOf(
            diary.entryDate,
            diary.entryTime.orEmpty(),
            diary.title,
            diary.mood.orEmpty(),
            diary.weather.orEmpty(),
            diary.locationName.orEmpty(),
            (diary.isFavorite == 1).toString(),
            record.tags.joinToString("|") { it.name },
            diary.contentMd
        )
    }
    return rows.joinToString("\n") { row -> row.joinToString(",") { it.csvEscape() } }
}

private fun List<DiaryExportRecord>.toMarkdown(): String {
    return buildString {
        appendLine("# Flux Diary Export")
        appendLine()
        this@toMarkdown.forEach { record ->
            val diary = record.diary
            appendLine("## ${diary.entryDate}${diary.entryTime?.let { " $it" }.orEmpty()}")
            if (diary.title.isNotBlank()) appendLine("### ${diary.title}")
            val meta: List<String> = listOfNotNull(
                diary.mood?.let { "心情：$it" },
                diary.weather?.let { "天气：$it" },
                diary.locationName?.let { "位置：$it" },
                record.tags.takeIf { it.isNotEmpty() }?.joinToString("、", prefix = "标签：") { it.name },
                if (diary.isFavorite == 1) "收藏" else null
            )
            if (meta.isNotEmpty()) appendLine("> ${meta.joinToString(" ｜ ")}")
            appendLine()
            appendLine(diary.contentMd)
            appendLine()
        }
    }
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
}
