package com.example.flux.feature.diary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import com.example.flux.core.domain.diary.ExportDiariesUseCase

data class DiaryFilterOptions(
    val months: List<String> = emptyList(),
    val years: List<String> = emptyList()
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val exportDiariesUseCase: ExportDiariesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isFavoriteFilter = MutableStateFlow(false)
    val isFavoriteFilter = _isFavoriteFilter.asStateFlow()

    private val _moodFilter = MutableStateFlow<String?>(null)
    val moodFilter = _moodFilter.asStateFlow()

    private val _monthFilter = MutableStateFlow<String?>(null)
    val monthFilter = _monthFilter.asStateFlow()

    private val _yearFilter = MutableStateFlow<String?>(null)
    val yearFilter = _yearFilter.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val diaries: StateFlow<List<DiaryEntity>> = combine(
        _searchQuery,
        _isFavoriteFilter,
        _moodFilter,
        _monthFilter,
        _yearFilter
    ) { query, isFav, mood, month, year ->
        DiaryFilters(query, isFav, mood, month, year)
    }.flatMapLatest { filters ->
        val baseFlow = if (filters.query.isBlank()) {
            diaryRepository.getActiveDiaries()
        } else {
            diaryRepository.searchDiaries(filters.query)
        }
        baseFlow.map { list ->
            list.filter { diary ->
                val matchFav = !filters.isFavorite || diary.isFavorite == 1
                val matchMood = filters.mood == null || diary.mood == filters.mood
                val matchMonth = filters.month == null || diary.entryDate.startsWith(filters.month)
                val matchYear = filters.year == null || diary.entryDate.startsWith(filters.year)
                matchFav && matchMood && matchMonth && matchYear
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filterOptions: StateFlow<DiaryFilterOptions> = diaryRepository.getActiveDiaries()
        .map { diaries ->
            DiaryFilterOptions(
                months = diaries.mapNotNull { it.entryDate.takeIf { date -> date.length >= 7 }?.take(7) }.distinct().sortedDescending(),
                years = diaries.mapNotNull { it.entryDate.takeIf { date -> date.length >= 4 }?.take(4) }.distinct().sortedDescending()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DiaryFilterOptions()
        )

    val onThisDayDiaries: StateFlow<List<DiaryEntity>> = run {
        val today = com.example.flux.core.util.TimeUtil.getCurrentDate() // returns "YYYY-MM-DD"
        val currentYear = today.substring(0, 4)
        val monthDay = today.substring(5, 10)
        diaryRepository.getOnThisDayDiaries(monthDay, currentYear)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFavoriteFilter(enabled: Boolean) {
        _isFavoriteFilter.value = enabled
    }

    fun setMoodFilter(mood: String?) {
        _moodFilter.value = mood
    }

    fun setMonthFilter(month: String?) {
        _monthFilter.value = month
        if (month != null) {
            _yearFilter.value = null
        }
    }

    fun setYearFilter(year: String?) {
        _yearFilter.value = year
        if (year != null) {
            _monthFilter.value = null
        }
    }

    fun clearFilters() {
        _isFavoriteFilter.value = false
        _moodFilter.value = null
        _monthFilter.value = null
        _yearFilter.value = null
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            ids.forEach { id ->
                diaryRepository.softDeleteDiary(id)
            }
            clearSelection()
        }
    }

    fun exportSelectedToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val ids = _selectedIds.value
            if (ids.isNotEmpty()) {
                exportDiariesUseCase(context, ids, uri)
                clearSelection()
            }
        }
    }
}

private data class DiaryFilters(
    val query: String,
    val isFavorite: Boolean,
    val mood: String?,
    val month: String?,
    val year: String?
)
