package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao
) {
    fun getActiveDiaries(): Flow<List<DiaryEntity>> {
        return diaryDao.getActiveDiaries()
    }

    fun getDiaryFlowByDate(date: String): Flow<DiaryEntity?> {
        return diaryDao.getDiaryFlowByDate(date)
    }

    fun searchDiaries(query: String): Flow<List<DiaryEntity>> {
        // FTS syntax needs * for prefix matching usually, or we can format it here
        val formattedQuery = "*$query*"
        return diaryDao.searchDiaries(formattedQuery)
    }

    fun getOnThisDayDiaries(monthDay: String, currentYear: String): Flow<List<DiaryEntity>> {
        return diaryDao.getOnThisDayDiaries(monthDay, currentYear)
    }

    fun getDeletedDiaries(): Flow<List<DiaryEntity>> {
        return diaryDao.getDeletedDiaries()
    }

    suspend fun getDiaryById(id: String): DiaryEntity? {
        return diaryDao.getDiaryById(id)
    }

    suspend fun getActiveDiaryByDate(date: String): DiaryEntity? {
        return diaryDao.getActiveDiaryByDate(date)
    }

    suspend fun saveDiary(diary: DiaryEntity) {
        val existing = diaryDao.getActiveDiaryByDate(diary.entryDate)
        if (existing != null && existing.id != diary.id && diary.deletedAt == null) {
            replaceDiaryRecord(
                existing.copy(
                    title = diary.title,
                    entryTime = diary.entryTime,
                    contentMd = diary.contentMd,
                    mood = diary.mood,
                    weather = diary.weather,
                    locationName = diary.locationName,
                    isFavorite = diary.isFavorite,
                    wordCount = diary.wordCount,
                    updatedAt = diary.updatedAt,
                    version = maxOf(existing.version, diary.version)
                )
            )
        } else {
            replaceDiaryRecord(diary)
        }
    }

    suspend fun replaceDiaryRecord(diary: DiaryEntity) {
        diaryDao.insertDiary(diary)
        syncDiaryFts(diary)
    }

    suspend fun softDeleteDiary(id: String) {
        val timestamp = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
        diaryDao.softDeleteDiary(id, timestamp)
        diaryDao.deleteDiaryFts(id)
    }

    private suspend fun syncDiaryFts(diary: DiaryEntity) {
        diaryDao.deleteDiaryFts(diary.id)
        if (diary.deletedAt == null) {
            diaryDao.insertDiaryFts(
                diaryId = diary.id,
                entryDate = diary.entryDate,
                entryTime = diary.entryTime,
                contentMd = diary.contentMd,
                mood = diary.mood,
                weather = diary.weather,
                locationName = diary.locationName
            )
        }
    }
}
