package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM calendar_events WHERE deleted_at IS NULL ORDER BY start_at ASC")
    fun getActiveEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE deleted_at IS NULL AND substr(start_at, 1, 10) = :date ORDER BY start_at ASC")
    fun getEventsByDate(date: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun getDeletedEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE subscription_id = :subscriptionId")
    suspend fun getEventsBySubscription(subscriptionId: String): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE subscription_id = :subscriptionId AND external_uid = :externalUid")
    suspend fun deleteExternalEvent(subscriptionId: String, externalUid: String)

    @Query("DELETE FROM calendar_events WHERE subscription_id = :subscriptionId")
    suspend fun deleteExternalEventsBySubscription(subscriptionId: String)

    @Query("UPDATE calendar_events SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDeleteEvent(id: String, timestamp: String)

    @Query("UPDATE calendar_events SET deleted_at = NULL, updated_at = :timestamp WHERE id = :id")
    suspend fun restoreEvent(id: String, timestamp: String)
}
