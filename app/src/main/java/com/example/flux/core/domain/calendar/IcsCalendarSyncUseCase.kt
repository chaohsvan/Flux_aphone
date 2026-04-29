package com.example.flux.core.domain.calendar

import com.example.flux.core.database.dao.CalendarSubscriptionDao
import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import com.example.flux.core.util.TimeUtil
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IcsSyncResult(
    val subscriptionId: String,
    val downloaded: Boolean,
    val inserted: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val unchanged: Int = 0
)

class IcsCalendarSyncUseCase @Inject constructor(
    private val subscriptionDao: CalendarSubscriptionDao,
    private val eventDao: EventDao,
    private val downloader: IcsCalendarDownloader,
    private val parser: IcsCalendarParser
) {
    suspend fun syncAllEnabled(): List<IcsSyncResult> = withContext(Dispatchers.IO) {
        subscriptionDao.getEnabledSubscriptions().mapNotNull { subscription ->
            runCatching { syncSubscription(subscription) }
                .onFailure { throwable ->
                    subscriptionDao.updateSyncError(
                        id = subscription.id,
                        error = throwable.message ?: throwable::class.java.simpleName,
                        updatedAt = TimeUtil.getCurrentIsoTime()
                    )
                }
                .getOrNull()
        }
    }

    suspend fun syncSubscription(id: String): IcsSyncResult = withContext(Dispatchers.IO) {
        val subscription = subscriptionDao.getSubscriptionById(id) ?: error("Calendar subscription not found")
        syncSubscription(subscription)
    }

    private suspend fun syncSubscription(subscription: CalendarSubscriptionEntity): IcsSyncResult {
        val download = downloader.download(
            url = subscription.icsUrl,
            etag = subscription.etag,
            lastModified = subscription.lastModifiedHeader
        )
        val now = TimeUtil.getCurrentIsoTime()
        return when (download) {
            is IcsDownloadResult.NotModified -> {
                subscriptionDao.updateSyncMetadata(
                    id = subscription.id,
                    lastSyncTime = now,
                    etag = download.etag,
                    lastModifiedHeader = download.lastModified,
                    updatedAt = now
                )
                IcsSyncResult(subscription.id, downloaded = false)
            }
            is IcsDownloadResult.Downloaded -> {
                val parsedEvents = parser.parse(download.body)
                val existingByUid = eventDao.getEventsBySubscription(subscription.id)
                    .mapNotNull { event -> event.externalUid?.let { it to event } }
                    .toMap()
                val parsedUids = parsedEvents.map { it.uid }.toSet()
                var inserted = 0
                var updated = 0
                var unchanged = 0

                parsedEvents.forEach { parsed ->
                    val existing = existingByUid[parsed.uid]
                    if (existing == null) {
                        eventDao.insertEvent(parsed.toEntity(subscription.id, now))
                        inserted += 1
                    } else if (existing.externalHash != parsed.hash || existing.deletedAt != null) {
                        eventDao.insertEvent(
                            parsed.toEntity(subscription.id, existing.createdAt).copy(
                                id = existing.id,
                                version = existing.version + 1,
                                updatedAt = now
                            )
                        )
                        updated += 1
                    } else {
                        unchanged += 1
                    }
                }

                val staleUids = existingByUid.keys - parsedUids
                if (parsedUids.isEmpty()) {
                    eventDao.deleteExternalEventsBySubscription(subscription.id)
                } else {
                    staleUids.forEach { uid -> eventDao.deleteExternalEvent(subscription.id, uid) }
                }

                subscriptionDao.updateSyncMetadata(
                    id = subscription.id,
                    lastSyncTime = now,
                    etag = download.etag,
                    lastModifiedHeader = download.lastModified,
                    updatedAt = now
                )
                IcsSyncResult(
                    subscriptionId = subscription.id,
                    downloaded = true,
                    inserted = inserted,
                    updated = updated,
                    deleted = staleUids.size,
                    unchanged = unchanged
                )
            }
        }
    }

    private fun ParsedIcsEvent.toEntity(subscriptionId: String, createdAt: String): CalendarEventEntity {
        val now = TimeUtil.getCurrentIsoTime()
        return CalendarEventEntity(
            id = "ics-${UUID.nameUUIDFromBytes("$subscriptionId:$uid".toByteArray(Charsets.UTF_8))}",
            title = title,
            description = description,
            startAt = startAt,
            endAt = endAt,
            allDay = if (allDay) 1 else 0,
            color = "#4A90E2",
            locationName = locationName,
            reminderMinutes = null,
            recurrenceRule = null,
            subscriptionId = subscriptionId,
            externalUid = uid,
            externalHash = hash,
            createdAt = createdAt,
            updatedAt = now,
            deletedAt = null,
            version = 1
        )
    }
}
