package com.example.flux.core.domain.diary

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.repository.DiaryRepository
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ExportDiariesUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(context: Context, ids: Set<String>, destUri: Uri) {
        val outStream: OutputStream? = context.contentResolver.openOutputStream(destUri)
        if (outStream == null) return

        ZipOutputStream(outStream).use { zos ->
            for (id in ids) {
                val diary = diaryRepository.getDiaryById(id) ?: continue
                
                // 1. Write Markdown file
                val safeTitle = diary.title.ifBlank { "无标题_${diary.entryDate}" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val mdFileName = "${diary.entryDate}_$safeTitle.md"
                
                zos.putNextEntry(ZipEntry(mdFileName))
                zos.write(diary.contentMd.toByteArray())
                zos.closeEntry()

                // 2. Extract and pack attachments
                // Match markdown images: ![alt](assets/attachments/image_name.jpg)
                val imgRegex = Regex("!\\[.*?\\]\\((assets/attachments/[^)]+)\\)")
                val matches = imgRegex.findAll(diary.contentMd)
                
                for (match in matches) {
                    val relativePath = match.groupValues[1] // "assets/attachments/image_name.jpg"
                    val localFile = File(context.filesDir, relativePath)
                    
                    if (localFile.exists()) {
                        // Create zip entry with the same relative path so markdown links work after unzip
                        val zipEntryName = relativePath
                        
                        try {
                            // Only put entry if not already exists (multiple diaries might share an image)
                            zos.putNextEntry(ZipEntry(zipEntryName))
                            FileInputStream(localFile).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            // Duplicate entry exception can be ignored or handled gracefully
                        }
                    }
                }
            }
        }
    }
}
