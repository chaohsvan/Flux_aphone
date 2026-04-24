package com.example.flux.core.domain.settings

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportBackupUseCase @Inject constructor(
    private val database: FluxDatabase
) {
    suspend operator fun invoke(context: Context, sourceUri: Uri) = withContext(Dispatchers.IO) {
        val restoreRoot = File(context.cacheDir, "flux_restore_${TimeUtil.generateUuid()}")
        val stagedData = File(restoreRoot, DataPaths.DATA_DIR_NAME)
        restoreRoot.deleteRecursively()
        restoreRoot.mkdirs()

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        if (entry.isDirectory) return@forEach
                        val cleanName = entry.name.replace('\\', '/').removePrefix("/")
                        if (!cleanName.startsWith("${DataPaths.DATA_DIR_NAME}/")) return@forEach
                        val target = File(restoreRoot, cleanName).canonicalFile
                        if (!target.path.startsWith(restoreRoot.canonicalPath)) {
                            error("Invalid backup path")
                        }
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            } ?: error("Unable to open backup file")

            val stagedDb = File(stagedData, DataPaths.DATABASE_NAME)
            if (!stagedDb.isFile || stagedDb.length() <= 0L) {
                error("Backup does not contain data/${DataPaths.DATABASE_NAME}")
            }

            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
            database.close()

            val dataDir = DataPaths.dataDir(context).canonicalFile
            val filesDir = context.filesDir.canonicalFile
            if (!dataDir.path.startsWith(filesDir.path)) error("Invalid data directory")

            val backupDir = File(
                filesDir,
                "data_before_restore_${TimeUtil.getCurrentDate()}_${TimeUtil.generateUuid().take(8)}"
            )
            if (dataDir.exists()) {
                dataDir.copyRecursively(backupDir, overwrite = true)
                dataDir.deleteRecursively()
            }
            stagedData.copyRecursively(dataDir, overwrite = true)
            backupDir
        } finally {
            restoreRoot.deleteRecursively()
        }
    }
}
