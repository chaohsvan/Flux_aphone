package com.example.flux.core.domain.diary

import android.content.Context
import com.example.flux.core.database.dao.DiaryDao
import java.io.File
import javax.inject.Inject

class AttachmentManagerUseCase @Inject constructor(
    private val diaryDao: DiaryDao
) {
    suspend fun getOrphanFiles(context: Context): List<File> {
        val attachmentsDir = File(context.filesDir, "assets/attachments")
        if (!attachmentsDir.exists() || !attachmentsDir.isDirectory) return emptyList()

        val allLocalFiles = attachmentsDir.listFiles()?.toList() ?: emptyList()
        if (allLocalFiles.isEmpty()) return emptyList()

        val allDiaries = diaryDao.getAllDiariesSnapshot()
        
        // Match standard attachments pattern
        val imgRegex = Regex("assets/attachments/([a-zA-Z0-9_.-]+)")
        val referencedFileNames = mutableSetOf<String>()

        allDiaries.forEach { diary ->
            val matches = imgRegex.findAll(diary.contentMd)
            matches.forEach { match ->
                referencedFileNames.add(match.groupValues[1])
            }
        }

        val orphans = allLocalFiles.filter { file ->
            !referencedFileNames.contains(file.name)
        }

        return orphans
    }

    suspend fun cleanOrphans(context: Context): Long {
        val orphans = getOrphanFiles(context)
        var freedSpace = 0L
        for (file in orphans) {
            val size = file.length()
            if (file.delete()) {
                freedSpace += size
            }
        }
        return freedSpace
    }
}
