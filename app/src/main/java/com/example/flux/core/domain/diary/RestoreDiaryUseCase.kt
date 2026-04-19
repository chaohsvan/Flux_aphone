package com.example.flux.core.domain.diary

import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.util.TimeUtil
import javax.inject.Inject

class RestoreDiaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(diaryId: String) {
        val diary = diaryRepository.getDiaryById(diaryId)
        if (diary != null && diary.deletedAt != null) {
            val now = TimeUtil.getCurrentIsoTime()
            val activeDiary = diaryRepository.getActiveDiaryByDate(diary.entryDate)

            if (activeDiary == null || activeDiary.id == diary.id) {
                diaryRepository.replaceDiaryRecord(
                    diary.copy(
                        deletedAt = null,
                        restoredAt = null,
                        restoredIntoId = null,
                        updatedAt = now
                    )
                )
            } else {
                val mergedContent = listOf(activeDiary.contentMd, diary.contentMd)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = "\n\n---\n\n")

                diaryRepository.replaceDiaryRecord(
                    activeDiary.copy(
                        title = activeDiary.title.ifBlank { diary.title },
                        entryTime = activeDiary.entryTime ?: diary.entryTime,
                        contentMd = mergedContent,
                        mood = activeDiary.mood ?: diary.mood,
                        weather = activeDiary.weather ?: diary.weather,
                        locationName = activeDiary.locationName ?: diary.locationName,
                        isFavorite = maxOf(activeDiary.isFavorite, diary.isFavorite),
                        wordCount = mergedContent.length,
                        updatedAt = now
                    )
                )
                diaryRepository.replaceDiaryRecord(
                    diary.copy(
                        restoredAt = now,
                        restoredIntoId = activeDiary.id,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
