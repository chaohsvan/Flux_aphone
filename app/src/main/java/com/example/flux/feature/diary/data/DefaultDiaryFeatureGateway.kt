package com.example.flux.feature.diary.data

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagSummary
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.domain.diary.DiaryExportFormat
import com.example.flux.core.domain.diary.ExportDiariesUseCase
import com.example.flux.feature.diary.domain.DiaryFeatureGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultDiaryFeatureGateway @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val exportDiariesUseCase: ExportDiariesUseCase
) : DiaryFeatureGateway {

    override fun getActiveDiaries(): Flow<List<DiaryEntity>> = diaryRepository.getActiveDiaries()

    override fun searchDiaries(query: String): Flow<List<DiaryEntity>> = diaryRepository.searchDiaries(query)

    override fun getDiaryIdsForTag(tagName: String): Flow<List<String>> = diaryRepository.getDiaryIdsForTag(tagName)

    override fun getActiveDiaryTagSummaries(): Flow<List<DiaryTagSummary>> = diaryRepository.getActiveDiaryTagSummaries()

    override fun getActiveTags(): Flow<List<DiaryTagEntity>> = diaryRepository.getActiveTags()

    override fun getOnThisDayDiaries(monthDay: String, currentYear: String): Flow<List<DiaryEntity>> {
        return diaryRepository.getOnThisDayDiaries(monthDay, currentYear)
    }

    override suspend fun getDiaryById(id: String): DiaryEntity? = diaryRepository.getDiaryById(id)

    override suspend fun getTagsForDiary(diaryId: String): List<DiaryTagEntity> = diaryRepository.getTagsForDiary(diaryId)

    override suspend fun getActiveDiaryByDate(entryDate: String): DiaryEntity? = diaryRepository.getActiveDiaryByDate(entryDate)

    override suspend fun saveDiary(diary: DiaryEntity, tags: List<String>) {
        diaryRepository.saveDiary(diary, tags)
    }

    override suspend fun softDeleteDiary(id: String) {
        diaryRepository.softDeleteDiary(id)
    }

    override suspend fun exportSelected(context: Context, ids: Set<String>, uri: Uri, format: DiaryExportFormat) {
        exportDiariesUseCase(context, ids, uri, format)
    }
}
