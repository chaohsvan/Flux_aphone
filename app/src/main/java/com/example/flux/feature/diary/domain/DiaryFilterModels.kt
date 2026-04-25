package com.example.flux.feature.diary.domain

data class DiaryFilterOptions(
    val months: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class DiaryFilters(
    val query: String,
    val isFavorite: Boolean,
    val mood: String?,
    val month: String?,
    val year: String?,
    val tag: String?
)
