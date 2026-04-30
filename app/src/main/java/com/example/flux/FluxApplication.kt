package com.example.flux

import android.app.Activity
import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.flux.core.database.FluxPrepackagedDatabaseNormalizer
import com.example.flux.core.sync.FluxSyncManager
import com.example.flux.core.sync.SyncStateStore
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
    @Inject lateinit var fluxSyncManager: FluxSyncManager
    @Inject lateinit var syncStateStore: SyncStateStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        DataDirectoryInitializer.ensure(this)
        normalizeExistingPrepackagedDatabase()
        IcsSyncWorker.schedule(this)
        registerForegroundSyncCallbacks()
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

    private fun registerForegroundSyncCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                val wasBackground = startedActivityCount == 0
                startedActivityCount += 1
                if (wasBackground) {
                    syncWithToast("正在同步数据...")
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (startedActivityCount == 0) {
                    syncWithToast("正在退出前同步...")
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun syncWithToast(startMessage: String) {
        appScope.launch {
            val result = runCatching {
                val config = syncStateStore.getConfig()
                if (!config.enabled || !config.isConfigured) return@launch
                showToast(startMessage)
                fluxSyncManager.syncNow()
            }
            result.fold(
                onSuccess = { showToast(it.message) },
                onFailure = { throwable ->
                    val message = throwable.message ?: "同步失败"
                    if (!message.contains("not enabled", ignoreCase = true) &&
                        !message.contains("incomplete", ignoreCase = true)
                    ) {
                        showToast(message)
                    }
                }
            )
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
