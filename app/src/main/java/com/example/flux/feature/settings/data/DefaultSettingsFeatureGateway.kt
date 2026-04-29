package com.example.flux.feature.settings.data

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.dao.CalendarSubscriptionDao
import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import com.example.flux.core.domain.calendar.IcsCalendarSyncUseCase
import com.example.flux.core.domain.calendar.IcsSyncResult
import com.example.flux.core.domain.settings.ExportBackupUseCase
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.domain.settings.ImportBackupUseCase
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.settings.AppPreferences
import com.example.flux.core.settings.WeatherAppBinding
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.settings.domain.SettingsFeatureGateway
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultSettingsFeatureGateway @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val observeTrashSummaryUseCase: ObserveTrashSummaryUseCase,
    private val appPreferences: AppPreferences,
    private val calendarSubscriptionDao: CalendarSubscriptionDao,
    private val eventDao: EventDao,
    private val icsCalendarSyncUseCase: IcsCalendarSyncUseCase
) : SettingsFeatureGateway {

    override fun observeTrashSummary(): Flow<TrashSummary> = observeTrashSummaryUseCase()

    override fun observeWeekStartDay(): Flow<Int> = appPreferences.observeWeekStartDay()

    override suspend fun setWeekStartDay(value: Int) {
        appPreferences.setWeekStartDay(value)
    }

    override fun observeWeatherAppBinding(): Flow<WeatherAppBinding?> {
        return appPreferences.observeWeatherAppBinding()
    }

    override suspend fun setWeatherAppBinding(binding: WeatherAppBinding) {
        appPreferences.setWeatherAppBinding(binding)
    }

    override suspend fun clearWeatherAppBinding() {
        appPreferences.clearWeatherAppBinding()
    }

    override fun observeCalendarSubscriptions(): Flow<List<CalendarSubscriptionEntity>> {
        return calendarSubscriptionDao.getSubscriptions()
    }

    override suspend fun addCalendarSubscription(name: String, icsUrl: String): CalendarSubscriptionEntity {
        val trimmedName = name.trim()
        val trimmedUrl = icsUrl.trim()
        validateSubscriptionInput(trimmedName, trimmedUrl)
        val now = TimeUtil.getCurrentIsoTime()
        val subscription = CalendarSubscriptionEntity(
            id = TimeUtil.generateUuid(),
            name = trimmedName,
            icsUrl = trimmedUrl,
            enabled = 1,
            createdAt = now,
            updatedAt = now
        )
        calendarSubscriptionDao.upsertSubscription(subscription)
        return subscription
    }

    override suspend fun updateCalendarSubscription(
        id: String,
        name: String,
        icsUrl: String
    ): CalendarSubscriptionEntity {
        val existing = calendarSubscriptionDao.getSubscriptionById(id) ?: error("订阅不存在")
        val trimmedName = name.trim()
        val trimmedUrl = icsUrl.trim()
        validateSubscriptionInput(trimmedName, trimmedUrl)
        val urlChanged = trimmedUrl != existing.icsUrl
        val updated = existing.copy(
            name = trimmedName,
            icsUrl = trimmedUrl,
            etag = if (urlChanged) null else existing.etag,
            lastModifiedHeader = if (urlChanged) null else existing.lastModifiedHeader,
            lastError = null,
            updatedAt = TimeUtil.getCurrentIsoTime()
        )
        calendarSubscriptionDao.upsertSubscription(updated)
        return updated
    }

    override suspend fun setCalendarSubscriptionEnabled(id: String, enabled: Boolean) {
        calendarSubscriptionDao.updateEnabled(
            id = id,
            enabled = if (enabled) 1 else 0,
            updatedAt = TimeUtil.getCurrentIsoTime()
        )
    }

    override suspend fun deleteCalendarSubscription(id: String) {
        eventDao.deleteExternalEventsBySubscription(id)
        calendarSubscriptionDao.deleteSubscription(id)
    }

    override suspend fun syncCalendarSubscription(id: String): IcsSyncResult {
        return icsCalendarSyncUseCase.syncSubscription(id)
    }

    override suspend fun exportBackup(context: Context, uri: Uri) {
        exportBackupUseCase(context, uri)
    }

    override suspend fun importBackup(context: Context, uri: Uri, mode: ImportBackupMode) {
        importBackupUseCase(context, uri, mode)
    }

    private fun validateSubscriptionInput(name: String, icsUrl: String) {
        require(name.isNotBlank()) { "订阅名称不能为空" }
        val parsedUrl = URL(icsUrl)
        require(parsedUrl.protocol == "https" || parsedUrl.protocol == "http") {
            "ICS 地址必须是 http 或 https"
        }
    }
}
