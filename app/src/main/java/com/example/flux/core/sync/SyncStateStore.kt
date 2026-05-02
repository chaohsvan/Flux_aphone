package com.example.flux.core.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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

    fun getConfig(): WebDavSyncConfig {
        return WebDavSyncConfig(
            enabled = true,
            baseUrl = JIANGUOYUN_WEBDAV_URL,
            username = preferences.getString(KEY_USERNAME, "").orEmpty(),
            password = secureStringStore.read(KEY_PASSWORD),
            remoteDir = preferences.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR).orEmpty().ifBlank { DEFAULT_REMOTE_DIR }
        )
    }

    fun saveConfig(config: WebDavSyncConfig) {
        secureStringStore.save(KEY_PASSWORD, config.password)
        preferences.edit()
            .putString(KEY_BASE_URL, JIANGUOYUN_WEBDAV_URL)
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_REMOTE_DIR, config.remoteDir.trim().trim('/').ifBlank { DEFAULT_REMOTE_DIR })
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "flux_webdav_preferences"
        const val DEFAULT_REMOTE_DIR = "FluxBackups"

        const val KEY_BASE_URL = "webdav_base_url"
        const val KEY_USERNAME = "webdav_username"
        const val KEY_PASSWORD = "webdav_password"
        const val KEY_REMOTE_DIR = "webdav_remote_dir"

        val CONFIG_KEYS = setOf(KEY_BASE_URL, KEY_USERNAME, KEY_REMOTE_DIR)
    }
}
