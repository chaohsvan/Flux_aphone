package com.example.flux

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.flux.core.database.FluxPrepackagedDatabaseNormalizer
import com.example.flux.core.reminder.ReminderRescheduler
import com.example.flux.core.sync.IcsSyncWorker
import com.example.flux.core.util.DataDirectoryInitializer
import com.example.flux.core.util.DataPaths
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FluxApplication : Application() {
    @Inject
    lateinit var reminderRescheduler: ReminderRescheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DataDirectoryInitializer.ensure(this)
        normalizeExistingPrepackagedDatabase()
        IcsSyncWorker.schedule(this)
        rescheduleReminders()
    }

    private fun rescheduleReminders() {
        applicationScope.launch {
            runCatching { reminderRescheduler.rescheduleAll() }
                .onFailure { throwable ->
                    Log.w("FluxApplication", "Unable to reschedule reminders", throwable)
                }
        }
    }

    private fun normalizeExistingPrepackagedDatabase() {
        val databaseFile = DataPaths.databaseFile(this)
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
