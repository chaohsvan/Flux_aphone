package com.example.flux.feature.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.DiaryEntity
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.example.flux.core.domain.diary.DiaryExportFormat
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.feature.diary.domain.DiaryFeatureGateway
import com.example.flux.feature.diary.domain.DiaryFilterOptions
import com.example.flux.feature.diary.domain.DiaryFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DiaryUiState(
    val diaries: List<DiaryEntity> = emptyList(),
    val onThisDayDiaries: List<DiaryEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isFavoriteFilter: Boolean = false,
    val moodFilter: String? = null,
    val monthFilter: String? = null,
    val yearFilter: String? = null,
    val tagFilter: String? = null,
    val diaryTags: Map<String, List<String>> = emptyMap(),
    val filterOptions: DiaryFilterOptions = DiaryFilterOptions(),
    val trashSummary: TrashSummary = TrashSummary()
) {
    val hasFilters: Boolean
        get() = isFavoriteFilter || moodFilter != null || monthFilter != null || yearFilter != null || tagFilter != null
}

private data class DiaryFilterSelectionState(
    val selectedIds: Set<String> = emptySet(),
    val isFavoriteFilter: Boolean = false,
    val moodFilter: String? = null,
    val monthFilter: String? = null,
    val yearFilter: String? = null
)

private data class DiaryContentState(
    val diaries: List<DiaryEntity> = emptyList(),
    val onThisDayDiaries: List<DiaryEntity> = emptyList(),
    val diaryTags: Map<String, List<String>> = emptyMap(),
    val filterOptions: DiaryFilterOptions = DiaryFilterOptions()
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryGateway: DiaryFeatureGateway,
    observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
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

    private val _tagFilter = MutableStateFlow<String?>(null)
    val tagFilter = _tagFilter.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    val trashSummary: StateFlow<TrashSummary> = observeTrashSummaryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

    private val rawFilters = combine(
        _searchQuery,
        _isFavoriteFilter,
        _moodFilter,
        _monthFilter,
        _yearFilter
    ) { query, isFav, mood, month, year ->
        DiaryFilters(query, isFav, mood, month, year, null)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val diaries: StateFlow<List<DiaryEntity>> = combine(rawFilters, _tagFilter) { filters, tag ->
        filters.copy(tag = tag)
    }.flatMapLatest { filters ->
        val baseFlow = if (filters.query.isBlank()) {
            diaryGateway.getActiveDiaries()
        } else {
            diaryGateway.searchDiaries(filters.query)
        }
        val taggedFlow = if (filters.tag == null) {
            baseFlow
        } else {
            combine(baseFlow, diaryGateway.getDiaryIdsForTag(filters.tag)) { list, ids ->
                val idSet = ids.toSet()
                list.filter { it.id in idSet }
            }
        }
        taggedFlow.map { list ->
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

    val diaryTags: StateFlow<Map<String, List<String>>> = diaryGateway.getActiveDiaryTagSummaries()
        .map { rows ->
            rows.groupBy { it.diaryId }
                .mapValues { entry -> entry.value.map { it.tagName }.distinct() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val filterOptions: StateFlow<DiaryFilterOptions> = combine(
        diaryGateway.getActiveDiaries(),
        diaryGateway.getActiveTags()
    ) { diaries, tags ->
            DiaryFilterOptions(
                months = diaries.mapNotNull { it.entryDate.takeIf { date -> date.length >= 7 }?.take(7) }.distinct().sortedDescending(),
                years = diaries.mapNotNull { it.entryDate.takeIf { date -> date.length >= 4 }?.take(4) }.distinct().sortedDescending(),
                tags = tags.map { it.name }
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
        diaryGateway.getOnThisDayDiaries(monthDay, currentYear)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val selectionState: StateFlow<DiaryFilterSelectionState> = combine(
        _selectedIds,
        _isFavoriteFilter,
        _moodFilter,
        _monthFilter,
        _yearFilter
    ) { selectedIds, isFavorite, mood, month, year ->
        DiaryFilterSelectionState(
            selectedIds = selectedIds,
            isFavoriteFilter = isFavorite,
            moodFilter = mood,
            monthFilter = month,
            yearFilter = year
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryFilterSelectionState()
    )

    private val contentState: StateFlow<DiaryContentState> = combine(
        diaries,
        onThisDayDiaries,
        diaryTags,
        filterOptions
    ) { diaries, onThisDayDiaries, diaryTags, filterOptions ->
        DiaryContentState(
            diaries = diaries,
            onThisDayDiaries = onThisDayDiaries,
            diaryTags = diaryTags,
            filterOptions = filterOptions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryContentState()
    )

    val uiState: StateFlow<DiaryUiState> = combine(
        contentState,
        selectionState,
        _tagFilter,
        trashSummary
    ) { contentState, selectionState, tag, trashSummary ->
        DiaryUiState(
            diaries = contentState.diaries,
            onThisDayDiaries = contentState.onThisDayDiaries,
            selectedIds = selectionState.selectedIds,
            isFavoriteFilter = selectionState.isFavoriteFilter,
            moodFilter = selectionState.moodFilter,
            monthFilter = selectionState.monthFilter,
            yearFilter = selectionState.yearFilter,
            tagFilter = tag,
            diaryTags = contentState.diaryTags,
            filterOptions = contentState.filterOptions,
            trashSummary = trashSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryUiState()
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

    fun setTagFilter(tag: String?) {
        _tagFilter.value = tag
    }

    fun clearFilters() {
        _isFavoriteFilter.value = false
        _moodFilter.value = null
        _monthFilter.value = null
        _yearFilter.value = null
        _tagFilter.value = null
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
                diaryGateway.softDeleteDiary(id)
            }
            clearSelection()
        }
    }

    fun exportSelectedToUri(context: Context, uri: Uri, format: DiaryExportFormat) {
        viewModelScope.launch {
            val ids = _selectedIds.value
            if (ids.isNotEmpty()) {
                diaryGateway.exportSelected(context, ids, uri, format)
                clearSelection()
            }
        }
    }
}
