package com.example.flux.core.database.entity

/**
 * The prepackaged database uses SQLite FTS5 for diary search. Room 2.6.x does
 * not expose an @Fts5 annotation, so this class documents the native table
 * shape while DiaryDao queries it directly.
 */
data class DiaryFtsEntity(
    val diaryId: String,
    val entryDate: String,
    val entryTime: String?,
    val contentMd: String,
    val mood: String?,
    val weather: String?,
    val locationName: String?,
    val tags: String?
)
