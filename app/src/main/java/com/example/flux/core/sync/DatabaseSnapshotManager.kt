package com.example.flux.core.sync

import android.content.Context
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.domain.settings.ImportBackupUseCase
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSnapshotManager @Inject constructor(
    private val database: FluxDatabase,
    private val importBackupUseCase: ImportBackupUseCase
) {
    fun currentDatabaseHash(context: Context): String {
        checkpoint()
        val databaseFile = DataPaths.databaseFile(context)
        return if (databaseFile.exists()) HashUtil.sha256(databaseFile) else ""
    }

    fun createSnapshot(context: Context, deviceId: String): DatabaseSnapshot {
        checkpoint()
        val databaseFile = DataPaths.databaseFile(context)
        require(databaseFile.isFile) { "Local database file does not exist" }

        val snapshotId = "${TimeUtil.getCurrentIsoTime().filter(Char::isLetterOrDigit)}_$deviceId"
        val zipFile = File(context.cacheDir, "flux_db_snapshot_$snapshotId.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("${DataPaths.DATA_DIR_NAME}/${DataPaths.DATABASE_NAME}"))
            databaseFile.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }

        val meta = DatabaseSnapshotMeta(
            snapshotId = snapshotId,
            createdAt = TimeUtil.getCurrentIsoTime(),
            deviceId = deviceId,
            sha256 = HashUtil.sha256(zipFile),
            sizeBytes = zipFile.length(),
            roomSchemaVersion = database.openHelper.readableDatabase.version
        )
        return DatabaseSnapshot(zipFile, meta)
    }

    suspend fun importSnapshot(context: Context, snapshotZip: File, mode: ImportBackupMode) {
        importBackupUseCase.fromFile(context, snapshotZip, mode)
    }

    private fun checkpoint() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
    }
}

data class DatabaseSnapshot(
    val file: File,
    val meta: DatabaseSnapshotMeta
)
