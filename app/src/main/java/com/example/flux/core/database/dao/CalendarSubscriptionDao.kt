package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarSubscriptionDao {
    @Query("SELECT * FROM calendar_subscription ORDER BY created_at ASC")
    fun getSubscriptions(): Flow<List<CalendarSubscriptionEntity>>

    @Query("SELECT * FROM calendar_subscription WHERE enabled = 1 ORDER BY created_at ASC")
    suspend fun getEnabledSubscriptions(): List<CalendarSubscriptionEntity>

    @Query("SELECT * FROM calendar_subscription WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: String): CalendarSubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(subscription: CalendarSubscriptionEntity)

    @Query("UPDATE calendar_subscription SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Int, updatedAt: String)

    @Query("DELETE FROM calendar_subscription WHERE id = :id")
    suspend fun deleteSubscription(id: String)

    @Query(
        """
        UPDATE calendar_subscription
        SET last_sync_time = :lastSyncTime,
            etag = :etag,
            last_modified_header = :lastModifiedHeader,
            last_error = NULL,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateSyncMetadata(
        id: String,
        lastSyncTime: String,
        etag: String?,
        lastModifiedHeader: String?,
        updatedAt: String
    )

    @Query("UPDATE calendar_subscription SET last_error = :error, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSyncError(id: String, error: String, updatedAt: String)
}
