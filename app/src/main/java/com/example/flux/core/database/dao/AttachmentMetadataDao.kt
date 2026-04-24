package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentMetadataDao {

    @Query("SELECT * FROM attachment_metadata ORDER BY modified_at DESC")
    fun observeAll(): Flow<List<AttachmentMetadataEntity>>

    @Query("SELECT * FROM attachment_metadata WHERE relative_path IN (:paths)")
    suspend fun getByPaths(paths: List<String>): List<AttachmentMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AttachmentMetadataEntity>)

    @Query("DELETE FROM attachment_metadata WHERE relative_path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM attachment_metadata WHERE relative_path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM attachment_metadata WHERE relative_path NOT IN (:paths)")
    suspend fun deleteNotIn(paths: List<String>)

    @Query("DELETE FROM attachment_metadata")
    suspend fun deleteAll()
}
