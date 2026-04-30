package com.example.flux.feature.settings.domain

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import com.example.flux.core.domain.calendar.IcsSyncResult
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.settings.WeatherAppBinding
import com.example.flux.core.sync.SyncRunResult
import com.example.flux.core.sync.SyncStatus
import com.example.flux.core.sync.WebDavSyncConfig
import kotlinx.coroutines.flow.Flow

interface SettingsFeatureGateway {
    fun observeTrashSummary(): Flow<TrashSummary>
    fun observeWeekStartDay(): Flow<Int>
    suspend fun setWeekStartDay(value: Int)
    fun observeReminderSoundEnabled(): Flow<Boolean>
    suspend fun setReminderSoundEnabled(enabled: Boolean)
    fun observeWeatherAppBinding(): Flow<WeatherAppBinding?>
    suspend fun setWeatherAppBinding(binding: WeatherAppBinding)
    suspend fun clearWeatherAppBinding()
    fun observeCalendarSubscriptions(): Flow<List<CalendarSubscriptionEntity>>
    suspend fun addCalendarSubscription(name: String, icsUrl: String): CalendarSubscriptionEntity
    suspend fun updateCalendarSubscription(id: String, name: String, icsUrl: String): CalendarSubscriptionEntity
    suspend fun setCalendarSubscriptionEnabled(id: String, enabled: Boolean)
    suspend fun deleteCalendarSubscription(id: String)
    suspend fun syncCalendarSubscription(id: String): IcsSyncResult
    suspend fun exportBackup(context: Context, uri: Uri)
    suspend fun importBackup(context: Context, uri: Uri, mode: ImportBackupMode)
    fun observeSyncConfig(): Flow<WebDavSyncConfig>
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun saveSyncConfig(config: WebDavSyncConfig)
    suspend fun testSyncConnection(config: WebDavSyncConfig): Boolean
    suspend fun syncNow(): SyncRunResult
}
