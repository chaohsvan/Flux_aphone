package com.example.flux.core.domain.settings

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.util.DataPaths
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportBackupUseCase @Inject constructor(
    private val database: FluxDatabase
) {
    suspend operator fun invoke(context: Context, destUri: Uri) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }

        context.contentResolver.openOutputStream(destUri)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                zip.addDirectory(DataPaths.dataDir(context), DataPaths.DATA_DIR_NAME)
            }
        } ?: error("Unable to create backup file")
    }

    private fun ZipOutputStream.addFile(file: File, entryName: String) {
        if (!file.exists() || !file.isFile) return
        if (file.name.endsWith("-wal") || file.name.endsWith("-shm")) return

        putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun ZipOutputStream.addDirectory(directory: File, entryRoot: String) {
        if (!directory.exists() || !directory.isDirectory) return
        directory.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relative = file.relativeTo(directory).invariantSeparatorsPath
                addFile(file, "$entryRoot/$relative")
            }
    }
}
