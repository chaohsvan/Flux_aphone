package com.example.flux.feature.trash.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.domain.diary.AttachmentKind
import com.example.flux.core.domain.diary.AttachmentDeleteResult
import com.example.flux.core.domain.diary.AttachmentManagerUseCase
import com.example.flux.core.domain.diary.ManagedAttachment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AttachmentReferenceFilter {
    ALL,
    REFERENCED,
    UNREFERENCED
}

enum class AttachmentSortMode {
    NAME,
    SIZE_DESC,
    SIZE_ASC,
    DATE_DESC,
    DATE_ASC,
    REFERENCE_COUNT
}

enum class AttachmentSizeFilter {
    ALL,
    UNDER_1_MB,
    MB_1_TO_10,
    ABOVE_10_MB
}

enum class AttachmentDateFilter {
    ALL,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_YEAR,
    BEFORE_THIS_YEAR
}

private data class PrimaryAttachmentFilters(
    val kind: AttachmentKind?,
    val reference: AttachmentReferenceFilter,
    val sort: AttachmentSortMode,
    val query: String
)

private data class SecondaryAttachmentFilters(
    val size: AttachmentSizeFilter,
    val date: AttachmentDateFilter
)

@HiltViewModel
class AttachmentManagerViewModel @Inject constructor(
    private val attachmentManagerUseCase: AttachmentManagerUseCase
) : ViewModel() {

    private val _attachments = MutableStateFlow<List<ManagedAttachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    private val _kindFilter = MutableStateFlow<AttachmentKind?>(null)
    val kindFilter = _kindFilter.asStateFlow()

    private val _referenceFilter = MutableStateFlow(AttachmentReferenceFilter.ALL)
    val referenceFilter = _referenceFilter.asStateFlow()

    private val _sortMode = MutableStateFlow(AttachmentSortMode.NAME)
    val sortMode = _sortMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sizeFilter = MutableStateFlow(AttachmentSizeFilter.ALL)
    val sizeFilter = _sizeFilter.asStateFlow()

    private val _dateFilter = MutableStateFlow(AttachmentDateFilter.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths = _selectedPaths.asStateFlow()

    private val primaryFilters = combine(
        _kindFilter,
        _referenceFilter,
        _sortMode,
        _searchQuery
    ) { kind, reference, sort, query ->
        PrimaryAttachmentFilters(
            kind = kind,
            reference = reference,
            sort = sort,
            query = query
        )
    }

    private val secondaryFilters = combine(
        _sizeFilter,
        _dateFilter
    ) { size, date ->
        SecondaryAttachmentFilters(size = size, date = date)
    }

    val filteredAttachments = combine(
        _attachments,
        primaryFilters,
        secondaryFilters
    ) { files, primaryFilters, secondaryFilters ->
        val now = System.currentTimeMillis()
        val yearStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        files
            .filter { attachment -> primaryFilters.kind == null || attachment.kind == primaryFilters.kind }
            .filter { attachment ->
                when (primaryFilters.reference) {
                    AttachmentReferenceFilter.ALL -> true
                    AttachmentReferenceFilter.REFERENCED -> attachment.isReferenced
                    AttachmentReferenceFilter.UNREFERENCED -> !attachment.isReferenced
                }
            }
            .filter { attachment ->
                when (secondaryFilters.size) {
                    AttachmentSizeFilter.ALL -> true
                    AttachmentSizeFilter.UNDER_1_MB -> attachment.sizeBytes < 1024L * 1024L
                    AttachmentSizeFilter.MB_1_TO_10 -> attachment.sizeBytes in (1024L * 1024L) until (10L * 1024L * 1024L)
                    AttachmentSizeFilter.ABOVE_10_MB -> attachment.sizeBytes >= 10L * 1024L * 1024L
                }
            }
            .filter { attachment ->
                when (secondaryFilters.date) {
                    AttachmentDateFilter.ALL -> true
                    AttachmentDateFilter.LAST_7_DAYS -> attachment.modifiedAt >= now - 7L * 24L * 60L * 60L * 1000L
                    AttachmentDateFilter.LAST_30_DAYS -> attachment.modifiedAt >= now - 30L * 24L * 60L * 60L * 1000L
                    AttachmentDateFilter.THIS_YEAR -> attachment.modifiedAt >= yearStart
                    AttachmentDateFilter.BEFORE_THIS_YEAR -> attachment.modifiedAt < yearStart
                }
            }
            .filter { attachment ->
                val query = primaryFilters.query.trim()
                query.isBlank() ||
                    attachment.file.name.contains(query, ignoreCase = true) ||
                    attachment.relativePath.contains(query, ignoreCase = true) ||
                    attachment.sha256.contains(query, ignoreCase = true) ||
                    attachment.references.any { source ->
                        source.diaryDate.contains(query, ignoreCase = true) ||
                            source.diaryId.contains(query, ignoreCase = true) ||
                            source.snippet.contains(query, ignoreCase = true)
                    }
            }
            .let { result ->
                when (primaryFilters.sort) {
                    AttachmentSortMode.NAME -> result.sortedBy { it.file.name.lowercase() }
                    AttachmentSortMode.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
                    AttachmentSortMode.SIZE_ASC -> result.sortedBy { it.sizeBytes }
                    AttachmentSortMode.DATE_DESC -> result.sortedByDescending { it.modifiedAt }
                    AttachmentSortMode.DATE_ASC -> result.sortedBy { it.modifiedAt }
                    AttachmentSortMode.REFERENCE_COUNT -> result.sortedByDescending { it.references.size }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _freedSpaceBytes = MutableStateFlow(0L)
    val freedSpaceBytes = _freedSpaceBytes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun scan(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            _attachments.value = attachmentManagerUseCase.scanAttachments(context)
            pruneSelection()
            _isScanning.value = false
        }
    }

    fun clean(context: Context, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isScanning.value = true
            val freed = attachmentManagerUseCase.cleanOrphans(context)
            _freedSpaceBytes.value = freed
            _attachments.value = attachmentManagerUseCase.scanAttachments(context)
            pruneSelection()
            _isScanning.value = false
            onDone(freed)
        }
    }

    fun setKindFilter(kind: AttachmentKind?) {
        _kindFilter.value = kind
    }

    fun setReferenceFilter(filter: AttachmentReferenceFilter) {
        _referenceFilter.value = filter
    }

    fun setSortMode(mode: AttachmentSortMode) {
        _sortMode.value = mode
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSizeFilter(filter: AttachmentSizeFilter) {
        _sizeFilter.value = filter
    }

    fun setDateFilter(filter: AttachmentDateFilter) {
        _dateFilter.value = filter
    }

    fun toggleSelectionMode() {
        val enabled = !_selectionMode.value
        _selectionMode.value = enabled
        if (!enabled) {
            _selectedPaths.value = emptySet()
        }
    }

    fun toggleSelection(relativePath: String) {
        _selectedPaths.value = _selectedPaths.value.toMutableSet().apply {
            if (!add(relativePath)) remove(relativePath)
        }
    }

    fun selectAllFiltered() {
        _selectionMode.value = true
        _selectedPaths.value = filteredAttachments.value.map { it.relativePath }.toCollection(linkedSetOf())
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun deleteOne(
        attachment: ManagedAttachment,
        allowReferenced: Boolean = false,
        onDone: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val freed = attachmentManagerUseCase.deleteAttachment(attachment, allowReferenced)
            if (freed > 0) {
                _freedSpaceBytes.value += freed
                _attachments.value = _attachments.value.filterNot { it.relativePath == attachment.relativePath }
                pruneSelection()
            }
            onDone(freed)
        }
    }

    fun deleteSelected(
        context: Context,
        allowReferenced: Boolean,
        onDone: (AttachmentDeleteResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val selected = _attachments.value.filter { it.relativePath in _selectedPaths.value }
            val result = attachmentManagerUseCase.deleteAttachments(context, selected, allowReferenced)
            if (result.deletedCount > 0) {
                _freedSpaceBytes.value += result.freedSpaceBytes
                val deletedPaths = result.deletedPaths.toHashSet()
                _attachments.value = _attachments.value.filterNot { it.relativePath in deletedPaths }
                pruneSelection()
            }
            if (_selectedPaths.value.isEmpty()) {
                _selectionMode.value = false
            }
            onDone(result)
        }
    }

    private fun pruneSelection() {
        val alivePaths = _attachments.value.map { it.relativePath }.toHashSet()
        _selectedPaths.value = _selectedPaths.value.filterTo(linkedSetOf()) { it in alivePaths }
        if (_selectedPaths.value.isEmpty() && _selectionMode.value && _attachments.value.isEmpty()) {
            _selectionMode.value = false
        }
    }
}
