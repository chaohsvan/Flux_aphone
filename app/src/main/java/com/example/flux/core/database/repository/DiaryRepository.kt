package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagLinkEntity
import com.example.flux.core.database.entity.DiaryTagSummary
import com.example.flux.core.util.TimeUtil
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
        val formattedQuery = query
            .trim()
            .split(Regex("\\s+"))
            .map { term ->
                term
                    .replace("\"", "")
                    .replace("*", "")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        return diaryDao.searchDiaries(formattedQuery)
    }

    fun getOnThisDayDiaries(monthDay: String, currentYear: String): Flow<List<DiaryEntity>> {
        return diaryDao.getOnThisDayDiaries(monthDay, currentYear)
    }

    fun getDeletedDiaries(): Flow<List<DiaryEntity>> {
        return diaryDao.getDeletedDiaries()
    }

    fun getActiveTags(): Flow<List<DiaryTagEntity>> {
        return diaryDao.getActiveTags()
    }

    fun getActiveDiaryTagSummaries(): Flow<List<DiaryTagSummary>> {
        return diaryDao.getActiveDiaryTagSummaries()
    }

    fun getDiaryIdsForTag(tagName: String): Flow<List<String>> {
        return diaryDao.getDiaryIdsForTag(tagName)
    }

    suspend fun getDiaryById(id: String): DiaryEntity? {
        return diaryDao.getDiaryById(id)
    }

    suspend fun getTagsForDiary(diaryId: String): List<DiaryTagEntity> {
        return diaryDao.getTagsForDiary(diaryId)
    }

    suspend fun getActiveDiaryByDate(date: String): DiaryEntity? {
        return diaryDao.getActiveDiaryByDate(date)
    }

    suspend fun saveDiary(diary: DiaryEntity, tagNames: List<String>? = null) {
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
                ),
                tagNames
            )
        } else {
            replaceDiaryRecord(diary, tagNames)
        }
    }

    suspend fun replaceDiaryRecord(diary: DiaryEntity, tagNames: List<String>? = null) {
        diaryDao.insertDiary(diary)
        if (tagNames != null) {
            updateDiaryTags(diary.id, tagNames)
        }
        syncDiaryFts(diary)
    }

    suspend fun softDeleteDiary(id: String) {
        val timestamp = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
        diaryDao.softDeleteDiary(id, timestamp)
        diaryDao.deleteDiaryFts(id)
    }

    private suspend fun updateDiaryTags(diaryId: String, tagNames: List<String>) {
        val normalizedTags = normalizeTags(tagNames)
        val now = TimeUtil.getCurrentIsoTime()
        diaryDao.softDeleteDiaryTagLinks(diaryId, now)
        normalizedTags.forEach { name ->
            val tag = diaryDao.getActiveTagByName(name) ?: DiaryTagEntity(
                id = TimeUtil.generateUuid(),
                name = name,
                color = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            ).also { diaryDao.insertTag(it) }

            diaryDao.insertDiaryTagLink(
                DiaryTagLinkEntity(
                    diaryId = diaryId,
                    tagId = tag.id,
                    createdAt = now,
                    deletedAt = null
                )
            )
        }
    }

    private suspend fun syncDiaryFts(diary: DiaryEntity) {
        diaryDao.deleteDiaryFts(diary.id)
        if (diary.deletedAt == null) {
            val tags = diaryDao.getTagsForDiary(diary.id).joinToString(" ") { it.name }
            diaryDao.insertDiaryFts(
                diaryId = diary.id,
                entryDate = diary.entryDate,
                entryTime = diary.entryTime,
                contentMd = diary.contentMd,
                mood = diary.mood,
                weather = diary.weather,
                locationName = diary.locationName,
                tags = tags
            )
        }
    }

    private fun normalizeTags(tagNames: List<String>): List<String> {
        return tagNames
            .flatMap { it.split(Regex("[,;#\\s\\uFF0C\\uFF1B\\u3001]+")) }
            .map { it.trim().trimStart('#').take(24) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(12)
    }
}
