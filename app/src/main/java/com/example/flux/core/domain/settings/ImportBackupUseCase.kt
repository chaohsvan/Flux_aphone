package com.example.flux.core.domain.settings

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportBackupUseCase @Inject constructor(
    private val database: FluxDatabase
) {
    suspend operator fun invoke(
        context: Context,
        sourceUri: Uri,
        mode: ImportBackupMode = ImportBackupMode.Replace
    ) = withContext(Dispatchers.IO) {
        restoreFromInput(context, mode) {
            context.contentResolver.openInputStream(sourceUri) ?: error("Unable to open backup file")
        }
    }

    suspend fun fromFile(
        context: Context,
        sourceFile: File,
        mode: ImportBackupMode = ImportBackupMode.Replace
    ) = withContext(Dispatchers.IO) {
        restoreFromInput(context, mode) {
            sourceFile.inputStream()
        }
    }

    private fun restoreFromInput(
        context: Context,
        mode: ImportBackupMode,
        openInput: () -> InputStream
    ) {
        val restoreRoot = File(context.cacheDir, "flux_restore_${TimeUtil.generateUuid()}")
        val stagedData = File(restoreRoot, DataPaths.DATA_DIR_NAME)
        restoreRoot.deleteRecursively()
        restoreRoot.mkdirs()

        try {
            openInput().use { input ->
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
            }

            val stagedDb = File(stagedData, DataPaths.DATABASE_NAME)
            if (!stagedDb.isFile || stagedDb.length() <= 0L) {
                error("Backup does not contain data/${DataPaths.DATABASE_NAME}")
            }

            when (mode) {
                ImportBackupMode.Replace -> replaceDataDirectory(context, stagedData)
                ImportBackupMode.Merge -> mergeDataDirectory(context, stagedData, stagedDb)
            }
        } finally {
            restoreRoot.deleteRecursively()
        }
    }

    private fun replaceDataDirectory(context: Context, stagedData: File): File {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
        database.close()

        val dataDir = DataPaths.dataDir(context).canonicalFile
        val filesDir = context.filesDir.canonicalFile
        if (!dataDir.path.startsWith(filesDir.path)) error("Invalid data directory")

        val backupDir = currentDataSnapshotDir(filesDir)
        if (dataDir.exists()) {
            dataDir.copyRecursively(backupDir, overwrite = true)
            dataDir.deleteRecursively()
        }
        stagedData.copyRecursively(dataDir, overwrite = true)
        return backupDir
    }

    private fun mergeDataDirectory(context: Context, stagedData: File, stagedDb: File): File {
        val dataDir = DataPaths.dataDir(context).canonicalFile
        val filesDir = context.filesDir.canonicalFile
        if (!dataDir.path.startsWith(filesDir.path)) error("Invalid data directory")

        val backupDir = currentDataSnapshotDir(filesDir)
        if (dataDir.exists()) {
            dataDir.copyRecursively(backupDir, overwrite = true)
        }

        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
        mergeDatabase(stagedDb)
        copyIncrementalFiles(stagedData, dataDir)
        return backupDir
    }

    private fun mergeDatabase(stagedDb: File) {
        val db = database.openHelper.writableDatabase
        val backupPath = stagedDb.canonicalPath.replace("'", "''")
        db.execSQL("ATTACH DATABASE '$backupPath' AS backup")
        try {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL("BEGIN TRANSACTION")
            try {
                MERGE_TABLES.forEach { table ->
                    mergeTable(table)
                }
                db.execSQL("COMMIT")
            } catch (throwable: Throwable) {
                db.execSQL("ROLLBACK")
                throw throwable
            } finally {
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        } finally {
            db.execSQL("DETACH DATABASE backup")
        }
    }

    private fun mergeTable(table: String) {
        val db = database.openHelper.writableDatabase
        if (!db.tableExists("main", table) || !db.tableExists("backup", table)) return
        val mainColumns = db.tableColumns("main", table)
        val backupColumns = db.tableColumns("backup", table)
        val columns = mainColumns.map { it.name }.filter { name -> backupColumns.any { it.name == name } }
        if (columns.isEmpty()) return

        val primaryKeys = mainColumns
            .filter { it.primaryKeyPosition > 0 && backupColumns.any { backup -> backup.name == it.name } }
            .sortedBy { it.primaryKeyPosition }
            .map { it.name }

        val quotedColumns = columns.joinToString(", ") { it.quotedIdentifier() }
        val tableName = table.quotedIdentifier()
        if (primaryKeys.isNotEmpty()) {
            val keyJoin = primaryKeys.joinToString(" AND ") { key ->
                "main.$tableName.${key.quotedIdentifier()} = backup.$tableName.${key.quotedIdentifier()}"
            }
            db.execSQL(
                """
                DELETE FROM main.$tableName
                WHERE EXISTS (
                    SELECT 1 FROM backup.$tableName
                    WHERE $keyJoin
                )
                """.trimIndent()
            )
        }
        db.execSQL(
            """
            INSERT OR IGNORE INTO main.$tableName ($quotedColumns)
            SELECT $quotedColumns FROM backup.$tableName
            """.trimIndent()
        )
    }

    private fun copyIncrementalFiles(stagedData: File, dataDir: File) {
        if (!stagedData.exists()) return
        stagedData.walkTopDown()
            .filter { it.isFile && it.name != DataPaths.DATABASE_NAME }
            .forEach { file ->
                val target = File(dataDir, file.relativeTo(stagedData).invariantSeparatorsPath).canonicalFile
                if (!target.path.startsWith(dataDir.canonicalPath)) error("Invalid backup path")
                target.parentFile?.mkdirs()
                file.copyTo(target, overwrite = true)
            }
    }

    private fun currentDataSnapshotDir(filesDir: File): File {
        return File(
            filesDir,
            "data_before_restore_${TimeUtil.getCurrentDate()}_${TimeUtil.generateUuid().take(8)}"
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.tableExists(schema: String, table: String): Boolean {
        return query(
            "SELECT name FROM $schema.sqlite_master WHERE type='table' AND name=?",
            arrayOf(table)
        ).use { cursor -> cursor.moveToFirst() }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.tableColumns(schema: String, table: String): List<TableColumn> {
        return query("PRAGMA $schema.table_info(${table.quotedSqlLiteral()})").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val pkIndex = cursor.getColumnIndex("pk")
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        TableColumn(
                            name = cursor.getString(nameIndex),
                            primaryKeyPosition = cursor.getInt(pkIndex)
                        )
                    )
                }
            }
        }
    }

    private fun String.quotedIdentifier(): String = "\"${replace("\"", "\"\"")}\""

    private fun String.quotedSqlLiteral(): String = "'${replace("'", "''")}'"

    private data class TableColumn(
        val name: String,
        val primaryKeyPosition: Int
    )

    companion object {
        private val MERGE_TABLES = listOf(
            "calendar_static_holidays",
            "calendar_holidays",
            "calendar_subscription",
            "calendar_events",
            "diaries",
            "diary_tags",
            "diary_tag_links",
            "diary_search_index",
            "todo_projects",
            "todos",
            "todo_subtasks",
            "todo_history",
            "attachment_metadata"
        )
    }
}

enum class ImportBackupMode {
    Replace,
    Merge
}
