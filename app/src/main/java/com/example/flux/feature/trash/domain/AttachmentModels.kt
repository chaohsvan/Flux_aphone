package com.example.flux.feature.trash.domain

import com.example.flux.core.domain.diary.AttachmentKind
import com.example.flux.core.domain.diary.ManagedAttachment

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

data class AttachmentFilterState(
    val kind: AttachmentKind? = null,
    val reference: AttachmentReferenceFilter = AttachmentReferenceFilter.ALL,
    val sort: AttachmentSortMode = AttachmentSortMode.NAME,
    val query: String = "",
    val size: AttachmentSizeFilter = AttachmentSizeFilter.ALL,
    val date: AttachmentDateFilter = AttachmentDateFilter.ALL
)

data class AttachmentUiState(
    val attachments: List<ManagedAttachment> = emptyList(),
    val filteredAttachments: List<ManagedAttachment> = emptyList(),
    val filterState: AttachmentFilterState = AttachmentFilterState(),
    val selectionMode: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val freedSpaceBytes: Long = 0L
)
