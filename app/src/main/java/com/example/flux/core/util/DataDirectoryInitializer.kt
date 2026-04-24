package com.example.flux.core.util

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import java.io.File

object DataDirectoryInitializer {
    fun ensure(context: Context) {
        DataPaths.ensureDataDir(context)
        migrateLegacyDatabase(context)
        migrateLegacyAttachments(context)
        seedStaticHolidaysFromAsset(context)
        copyAssetAttachments(context.assets, "attachments", DataPaths.attachmentsDir(context))
    }

    private fun migrateLegacyDatabase(context: Context) {
        val target = DataPaths.databaseFile(context)
        if (target.exists()) return

        val legacy = context.getDatabasePath(DataPaths.DATABASE_NAME)
        if (!legacy.exists()) return

        runCatching {
            SQLiteDatabase.openDatabase(
                legacy.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { database ->
                database.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { }
            }
        }

        target.parentFile?.mkdirs()
        legacy.copyTo(target, overwrite = false)
    }

    private fun migrateLegacyAttachments(context: Context) {
        val legacy = DataPaths.legacyAttachmentsDir(context)
        if (!legacy.exists() || !legacy.isDirectory) return

        val target = DataPaths.attachmentsDir(context)
        legacy.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val targetFile = File(target, file.relativeTo(legacy).invariantSeparatorsPath)
                if (!targetFile.exists()) {
                    targetFile.parentFile?.mkdirs()
                    file.copyTo(targetFile, overwrite = false)
                }
            }
    }

    private fun seedStaticHolidaysFromAsset(context: Context) {
        val target = DataPaths.databaseFile(context)
        if (!target.exists()) return

        val assetDatabase = File(context.cacheDir, "flux_asset_${TimeUtil.generateUuid()}.db")
        runCatching {
            context.assets.open(DataPaths.DATABASE_NAME).use { input ->
                assetDatabase.outputStream().use { output -> input.copyTo(output) }
            }

            SQLiteDatabase.openDatabase(
                target.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { database ->
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_static_holidays (
                        day TEXT NOT NULL PRIMARY KEY,
                        is_holiday INTEGER NOT NULL DEFAULT 1,
                        name TEXT,
                        source TEXT NOT NULL DEFAULT 'chinese-days',
                        updated_at TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                val hasStaticRows = database.rawQuery(
                    "SELECT COUNT(*) FROM calendar_static_holidays",
                    null
                ).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) > 0
                }
                if (hasStaticRows) return@use

                SQLiteDatabase.openDatabase(
                    assetDatabase.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use { source ->
                    source.rawQuery(
                        """
                        SELECT day, is_holiday, name, source, updated_at
                        FROM calendar_static_holidays
                        """.trimIndent(),
                        null
                    ).use { cursor ->
                        database.beginTransaction()
                        try {
                            val statement = database.compileStatement(
                                """
                                INSERT OR REPLACE INTO calendar_static_holidays(
                                    day, is_holiday, name, source, updated_at
                                ) VALUES (?, ?, ?, ?, ?)
                                """.trimIndent()
                            )
                            while (cursor.moveToNext()) {
                                statement.clearBindings()
                                statement.bindString(1, cursor.getString(0))
                                statement.bindLong(2, cursor.getLong(1))
                                if (cursor.isNull(2)) statement.bindNull(3) else statement.bindString(3, cursor.getString(2))
                                statement.bindString(4, cursor.getString(3))
                                statement.bindString(5, cursor.getString(4))
                                statement.executeInsert()
                            }
                            database.setTransactionSuccessful()
                        } finally {
                            database.endTransaction()
                        }
                    }
                }
            }
        }.also {
            assetDatabase.delete()
        }
    }

    private fun copyAssetAttachments(assetManager: AssetManager, assetPath: String, targetDir: File) {
        val children = assetManager.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            copyAssetFileIfMissing(assetManager, assetPath, targetDir)
            return
        }

        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            copyAssetAttachments(assetManager, childAssetPath, File(targetDir, child))
        }
    }

    private fun copyAssetFileIfMissing(assetManager: AssetManager, assetPath: String, targetFile: File) {
        if (targetFile.exists()) return

        runCatching {
            targetFile.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
