package com.example.flux.core.sync

import android.content.Context
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray

@Singleton
class SyncStateStore @Inject constructor(
    @ApplicationContext context: Context,
    private val secureStringStore: SecureStringStore
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun observeConfig(): Flow<WebDavSyncConfig> = callbackFlow {
        trySend(getConfig())
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in CONFIG_KEYS) trySend(getConfig())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun observeStatus(): Flow<SyncStatus> = callbackFlow {
        trySend(getStatus())
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in STATUS_KEYS) trySend(getStatus())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun getConfig(): WebDavSyncConfig {
        return WebDavSyncConfig(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            baseUrl = JIANGUOYUN_WEBDAV_URL,
            username = preferences.getString(KEY_USERNAME, "").orEmpty(),
            password = secureStringStore.read(KEY_PASSWORD),
            remoteDir = preferences.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR).orEmpty().ifBlank { DEFAULT_REMOTE_DIR }
        )
    }

    fun saveConfig(config: WebDavSyncConfig) {
        secureStringStore.save(KEY_PASSWORD, config.password)
        preferences.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_BASE_URL, JIANGUOYUN_WEBDAV_URL)
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_REMOTE_DIR, config.remoteDir.trim().trim('/').ifBlank { DEFAULT_REMOTE_DIR })
            .apply()
    }

    fun getStatus(): SyncStatus {
        return SyncStatus(
            isRunning = preferences.getBoolean(KEY_RUNNING, false),
            lastSyncAt = preferences.getString(KEY_LAST_SYNC_AT, "").orEmpty(),
            lastMessage = preferences.getString(KEY_LAST_MESSAGE, "").orEmpty(),
            lastError = preferences.getString(KEY_LAST_ERROR, "").orEmpty(),
            logs = getLogs()
        )
    }

    fun markRunning(running: Boolean) {
        preferences.edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun markSuccess(message: String) {
        appendLog("INFO", message)
        preferences.edit()
            .putBoolean(KEY_RUNNING, false)
            .putString(KEY_LAST_SYNC_AT, TimeUtil.getCurrentIsoTime())
            .putString(KEY_LAST_MESSAGE, message)
            .putString(KEY_LAST_ERROR, "")
            .apply()
    }

    fun markError(message: String) {
        appendLog("ERROR", message)
        preferences.edit()
            .putBoolean(KEY_RUNNING, false)
            .putString(KEY_LAST_ERROR, message)
            .apply()
    }

    fun appendLog(level: String, message: String) {
        val next = (listOf(SyncLogEntry(TimeUtil.getCurrentIsoTime(), level, message)) + getLogs())
            .take(MAX_LOG_COUNT)
        val json = JSONArray()
        next.forEach { json.put(it.toJson()) }
        preferences.edit().putString(KEY_LOGS, json.toString()).apply()
    }

    fun getLastAttachmentPaths(): Set<String> {
        val raw = preferences.getString(KEY_LAST_ATTACHMENT_PATHS, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split('\n').filter { it.isNotBlank() }.toSet()
    }

    fun updateLastAttachmentPaths(paths: Set<String>) {
        preferences.edit()
            .putString(KEY_LAST_ATTACHMENT_PATHS, paths.sorted().joinToString("\n"))
            .apply()
    }

    fun getDeviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val created = TimeUtil.generateUuid()
        preferences.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun getLastLocalDbHash(): String = preferences.getString(KEY_LAST_LOCAL_DB_HASH, "").orEmpty()

    fun getLastRemoteSnapshotId(): String = preferences.getString(KEY_LAST_REMOTE_SNAPSHOT_ID, "").orEmpty()

    fun updateDatabaseSyncState(localDbHash: String, remoteSnapshotId: String) {
        preferences.edit()
            .putString(KEY_LAST_LOCAL_DB_HASH, localDbHash)
            .putString(KEY_LAST_REMOTE_SNAPSHOT_ID, remoteSnapshotId)
            .apply()
    }

    private fun getLogs(): List<SyncLogEntry> {
        val raw = preferences.getString(KEY_LOGS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(SyncLogEntry.fromJson(array.getJSONObject(index)))
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES_NAME = "flux_sync_preferences"
        const val DEFAULT_REMOTE_DIR = "FluxSync"

        const val KEY_ENABLED = "sync_enabled"
        const val KEY_BASE_URL = "webdav_base_url"
        const val KEY_USERNAME = "webdav_username"
        const val KEY_PASSWORD = "webdav_password"
        const val KEY_REMOTE_DIR = "webdav_remote_dir"
        const val KEY_RUNNING = "sync_running"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LAST_MESSAGE = "last_message"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LAST_LOCAL_DB_HASH = "last_local_db_hash"
        const val KEY_LAST_REMOTE_SNAPSHOT_ID = "last_remote_snapshot_id"
        const val KEY_LAST_ATTACHMENT_PATHS = "last_attachment_paths"
        const val KEY_LOGS = "sync_logs"
        const val MAX_LOG_COUNT = 30

        val CONFIG_KEYS = setOf(KEY_ENABLED, KEY_BASE_URL, KEY_USERNAME, KEY_REMOTE_DIR)
        val STATUS_KEYS = setOf(KEY_RUNNING, KEY_LAST_SYNC_AT, KEY_LAST_MESSAGE, KEY_LAST_ERROR, KEY_LOGS)
    }
}
