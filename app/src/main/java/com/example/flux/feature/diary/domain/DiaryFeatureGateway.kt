package com.example.flux.feature.diary.domain

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagSummary
import com.example.flux.core.domain.diary.DiaryExportFormat
import kotlinx.coroutines.flow.Flow

interface DiaryFeatureGateway {
    fun getActiveDiaries(): Flow<List<DiaryEntity>>
    fun searchDiaries(query: String): Flow<List<DiaryEntity>>
    fun getDiaryIdsForTag(tagName: String): Flow<List<String>>
    fun getActiveDiaryTagSummaries(): Flow<List<DiaryTagSummary>>
    fun getActiveTags(): Flow<List<DiaryTagEntity>>
    fun getOnThisDayDiaries(monthDay: String, currentYear: String): Flow<List<DiaryEntity>>

    suspend fun getDiaryById(id: String): DiaryEntity?
    suspend fun getTagsForDiary(diaryId: String): List<DiaryTagEntity>
    suspend fun getActiveDiaryByDate(entryDate: String): DiaryEntity?
    suspend fun saveDiary(diary: DiaryEntity, tags: List<String>)
    suspend fun softDeleteDiary(id: String)
    suspend fun exportSelected(context: Context, ids: Set<String>, uri: Uri, format: DiaryExportFormat)
}
