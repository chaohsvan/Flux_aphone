package com.example.flux.feature.diary.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.repository.DiaryRepository
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryEditorUiState(
    val id: String? = null,
    val title: String = "",
    val contentMd: String = "",
    val entryDate: String = "",
    val entryTime: String? = null,
    val mood: String? = null,
    val weather: String? = null,
    val locationName: String? = null,
    val isFavorite: Boolean = false,
    val tagText: String = "",
    val createdAt: String? = null,
    val version: Int = 1,
    val isLoading: Boolean = false
)

@HiltViewModel
class DiaryEditorViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val diaryId: String? = savedStateHandle["diaryId"]

    private val _uiState = MutableStateFlow(DiaryEditorUiState(isLoading = true))
    val uiState: StateFlow<DiaryEditorUiState> = _uiState.asStateFlow()

    init {
        if (diaryId != null && diaryId != "new") {
            loadDiary(diaryId)
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entryDate = TimeUtil.getCurrentDate()
                )
            }
        }
    }

    private fun loadDiary(id: String) {
        viewModelScope.launch {
            val diary = diaryRepository.getDiaryById(id)
            if (diary != null) {
                val tags = diaryRepository.getTagsForDiary(id).joinToString(", ") { it.name }
                _uiState.update {
                    it.copy(
                        id = diary.id,
                        title = diary.title,
                        contentMd = diary.contentMd,
                        entryDate = diary.entryDate,
                        entryTime = diary.entryTime,
                        mood = diary.mood,
                        weather = diary.weather,
                        locationName = diary.locationName,
                        isFavorite = diary.isFavorite == 1,
                        tagText = tags,
                        createdAt = diary.createdAt,
                        version = diary.version,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(contentMd = newContent) }
    }

    fun updateEntryDate(newDate: String) {
        _uiState.update { it.copy(entryDate = newDate) }
    }

    fun updateEntryTime(newTime: String) {
        _uiState.update { it.copy(entryTime = newTime.ifBlank { null }) }
    }

    fun updateMood(newMood: String?) {
        _uiState.update { it.copy(mood = newMood) }
    }

    fun updateWeather(newWeather: String) {
        _uiState.update { it.copy(weather = newWeather.ifBlank { null }) }
    }

    fun updateLocation(newLocation: String) {
        _uiState.update { it.copy(locationName = newLocation.ifBlank { null }) }
    }

    fun updateTagText(newTagText: String) {
        _uiState.update { it.copy(tagText = newTagText) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun saveDiary() {
        val currentState = _uiState.value
        if (currentState.title.isBlank() && currentState.contentMd.isBlank()) return

        viewModelScope.launch {
            val now = TimeUtil.getCurrentIsoTime()
            val defaultTitle = currentState.contentMd
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.take(24)
                ?: "无标题"
            val entity = DiaryEntity(
                id = currentState.id ?: TimeUtil.generateUuid(),
                entryDate = currentState.entryDate.ifBlank { TimeUtil.getCurrentDate() },
                entryTime = currentState.entryTime,
                title = currentState.title.ifBlank { defaultTitle },
                contentMd = currentState.contentMd,
                mood = currentState.mood,
                weather = currentState.weather,
                locationName = currentState.locationName,
                isFavorite = if (currentState.isFavorite) 1 else 0,
                wordCount = currentState.contentMd.length,
                createdAt = currentState.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                version = currentState.version + 1,
                restoredAt = null,
                restoredIntoId = null
            )
            diaryRepository.saveDiary(entity, parseTags(currentState.tagText))
        }
    }

    fun deleteDiary() {
        val id = _uiState.value.id ?: return
        viewModelScope.launch {
            diaryRepository.softDeleteDiary(id)
        }
    }

    private fun parseTags(tagText: String): List<String> {
        return tagText
            .split(Regex("[,;#\\s\\uFF0C\\uFF1B\\u3001]+"))
            .map { it.trim().trimStart('#').take(24) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(12)
    }
}
