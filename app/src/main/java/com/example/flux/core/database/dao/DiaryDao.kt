package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.room.Update
import com.example.flux.core.database.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diaries WHERE deleted_at IS NULL ORDER BY entry_date DESC")
    fun getActiveDiaries(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries")
    suspend fun getAllDiariesSnapshot(): List<DiaryEntity>

    @Query("SELECT * FROM diaries WHERE entry_date = :date AND deleted_at IS NULL LIMIT 1")
    fun getDiaryFlowByDate(date: String): Flow<DiaryEntity?>

    @Query("SELECT * FROM diaries WHERE entry_date = :date AND deleted_at IS NULL LIMIT 1")
    suspend fun getActiveDiaryByDate(date: String): DiaryEntity?

    @Query("SELECT * FROM diaries WHERE deleted_at IS NULL AND substr(entry_date, 6, 5) = :monthDay AND substr(entry_date, 1, 4) != :currentYear ORDER BY entry_date DESC")
    fun getOnThisDayDiaries(monthDay: String, currentYear: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries WHERE id = :id LIMIT 1")
    suspend fun getDiaryById(id: String): DiaryEntity?

    @SkipQueryVerification
    @Query("""
        SELECT diaries.* FROM diaries 
        JOIN diaries_fts ON diaries.id = diaries_fts.diary_id 
        WHERE diaries_fts MATCH :query AND diaries.deleted_at IS NULL
    """)
    fun searchDiaries(query: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries WHERE deleted_at IS NOT NULL AND restored_into_id IS NULL ORDER BY deleted_at DESC")
    fun getDeletedDiaries(): Flow<List<DiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    @Query("UPDATE diaries SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDeleteDiary(id: String, timestamp: String)

    @SkipQueryVerification
    @Query("DELETE FROM diaries_fts WHERE diary_id = :diaryId")
    suspend fun deleteDiaryFts(diaryId: String)

    @SkipQueryVerification
    @Query("""
        INSERT INTO diaries_fts(diary_id, entry_date, entry_time, content_md, mood, weather, location_name, tags)
        VALUES(:diaryId, :entryDate, :entryTime, :contentMd, :mood, :weather, :locationName, '')
    """)
    suspend fun insertDiaryFts(
        diaryId: String,
        entryDate: String,
        entryTime: String?,
        contentMd: String,
        mood: String?,
        weather: String?,
        locationName: String?
    )
}
