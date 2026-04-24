package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachment_metadata",
    indices = [
        Index(name = "idx_attachment_metadata_kind", value = ["kind"]),
        Index(name = "idx_attachment_metadata_size", value = ["size_bytes"]),
        Index(name = "idx_attachment_metadata_modified", value = ["modified_at"]),
        Index(name = "idx_attachment_metadata_sha", value = ["sha256"])
    ]
)
data class AttachmentMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    val kind: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long,
    val sha256: String,
    @ColumnInfo(name = "reference_count", defaultValue = "0")
    val referenceCount: Int = 0,
    @ColumnInfo(name = "last_scanned_at")
    val lastScannedAt: String
)
