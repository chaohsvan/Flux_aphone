package com.example.flux

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.flux.core.database.FluxPrepackagedDatabaseNormalizer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FluxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        normalizeExistingPrepackagedDatabase()
    }

    private fun normalizeExistingPrepackagedDatabase() {
        val databaseFile = getDatabasePath("flux.db")
        if (!databaseFile.exists()) return

        SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use { database ->
            if (needsPrimaryKeyNormalization(database)) {
                FluxPrepackagedDatabaseNormalizer.normalize(database)
            }
        }
    }

    private fun needsPrimaryKeyNormalization(database: SQLiteDatabase): Boolean {
        return try {
            database.rawQuery("PRAGMA table_info(diaries)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val notNullIndex = cursor.getColumnIndex("notnull")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "id") {
                        return cursor.getInt(notNullIndex) == 0
                    }
                }
                false
            }
        } catch (throwable: Throwable) {
            Log.w("FluxApplication", "Unable to inspect existing database schema", throwable)
            false
        }
    }
}
