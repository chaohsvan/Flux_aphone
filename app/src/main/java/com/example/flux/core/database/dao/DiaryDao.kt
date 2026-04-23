package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.room.Update
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagLinkEntity
import com.example.flux.core.database.entity.DiaryTagSummary
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
        JOIN diary_search_index ON diaries.id = diary_search_index.diary_id
        WHERE diaries.deleted_at IS NULL
          AND (
            lower(diary_search_index.title) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.entry_date) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.entry_time) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.content_md) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.mood) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.weather) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.location_name) LIKE '%' || lower(:query) || '%'
            OR lower(diary_search_index.tags) LIKE '%' || lower(:query) || '%'
          )
        ORDER BY diaries.entry_date DESC
    """)
    fun searchDiaries(query: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries WHERE deleted_at IS NOT NULL AND restored_into_id IS NULL ORDER BY deleted_at DESC")
    fun getDeletedDiaries(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_tags WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun getActiveTags(): Flow<List<DiaryTagEntity>>

    @Query("SELECT * FROM diary_tags WHERE lower(name) = lower(:name) AND deleted_at IS NULL LIMIT 1")
    suspend fun getActiveTagByName(name: String): DiaryTagEntity?

    @Query("""
        SELECT diary_tags.* FROM diary_tags
        JOIN diary_tag_links ON diary_tags.id = diary_tag_links.tag_id
        WHERE diary_tag_links.diary_id = :diaryId
          AND diary_tag_links.deleted_at IS NULL
          AND diary_tags.deleted_at IS NULL
        ORDER BY diary_tags.name COLLATE NOCASE ASC
    """)
    suspend fun getTagsForDiary(diaryId: String): List<DiaryTagEntity>

    @Query("""
        SELECT diary_tag_links.diary_id AS diary_id, diary_tags.name AS tag_name
        FROM diary_tag_links
        JOIN diary_tags ON diary_tags.id = diary_tag_links.tag_id
        JOIN diaries ON diaries.id = diary_tag_links.diary_id
        WHERE diary_tag_links.deleted_at IS NULL
          AND diary_tags.deleted_at IS NULL
          AND diaries.deleted_at IS NULL
        ORDER BY diary_tags.name COLLATE NOCASE ASC
    """)
    fun getActiveDiaryTagSummaries(): Flow<List<DiaryTagSummary>>

    @Query("""
        SELECT diary_tag_links.diary_id
        FROM diary_tag_links
        JOIN diary_tags ON diary_tags.id = diary_tag_links.tag_id
        JOIN diaries ON diaries.id = diary_tag_links.diary_id
        WHERE lower(diary_tags.name) = lower(:tagName)
          AND diary_tag_links.deleted_at IS NULL
          AND diary_tags.deleted_at IS NULL
          AND diaries.deleted_at IS NULL
    """)
    fun getDiaryIdsForTag(tagName: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: DiaryTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryTagLink(link: DiaryTagLinkEntity)

    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    @Query("UPDATE diaries SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDeleteDiary(id: String, timestamp: String)

    @Query("UPDATE diary_tag_links SET deleted_at = :deletedAt WHERE diary_id = :diaryId AND deleted_at IS NULL")
    suspend fun softDeleteDiaryTagLinks(diaryId: String, deletedAt: String)

    @SkipQueryVerification
    @Query("DELETE FROM diary_search_index WHERE diary_id = :diaryId")
    suspend fun deleteDiaryFts(diaryId: String)

    @SkipQueryVerification
    @Query("""
        INSERT OR REPLACE INTO diary_search_index(
            diary_id, entry_date, entry_time, title, content_md, mood, weather, location_name, tags
        )
        VALUES(:diaryId, :entryDate, :entryTime, :title, :contentMd, :mood, :weather, :locationName, :tags)
    """)
    suspend fun insertDiaryFts(
        diaryId: String,
        entryDate: String,
        entryTime: String?,
        title: String,
        contentMd: String,
        mood: String?,
        weather: String?,
        locationName: String?,
        tags: String
    )
}
