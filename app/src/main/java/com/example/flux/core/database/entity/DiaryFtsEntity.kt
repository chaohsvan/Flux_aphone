package com.example.flux.core.database.entity

/**
 * Documents the app-managed diary search index shape. It is a normal SQLite
 * table rather than a virtual FTS table so it works on Android builds without
 * optional SQLite search modules.
 */
data class DiaryFtsEntity(
    val diaryId: String,
    val entryDate: String,
    val entryTime: String?,
    val title: String,
    val contentMd: String,
    val mood: String?,
    val weather: String?,
    val locationName: String?,
    val tags: String?
)
