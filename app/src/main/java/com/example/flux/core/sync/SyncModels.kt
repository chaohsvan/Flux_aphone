package com.example.flux.core.sync

import org.json.JSONArray
import org.json.JSONObject

data class WebDavSyncConfig(
    val enabled: Boolean = false,
    val baseUrl: String = JIANGUOYUN_WEBDAV_URL,
    val username: String = "",
    val password: String = "",
    val remoteDir: String = "FluxSync"
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && remoteDir.isNotBlank()
}

const val JIANGUOYUN_WEBDAV_URL = "https://dav.jianguoyun.com/dav/"

data class SyncStatus(
    val isRunning: Boolean = false,
    val lastSyncAt: String = "",
    val lastMessage: String = "",
    val lastError: String = "",
    val logs: List<SyncLogEntry> = emptyList()
)

data class SyncLogEntry(
    val time: String,
    val level: String,
    val message: String
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("time", time)
            .put("level", level)
            .put("message", message)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncLogEntry {
            return SyncLogEntry(
                time = json.optString("time"),
                level = json.optString("level"),
                message = json.optString("message")
            )
        }
    }
}

data class SyncManifest(
    val protocolVersion: Int = 1,
    val latestDbSnapshotId: String = "",
    val latestDbPath: String = "",
    val latestDbHash: String = "",
    val updatedAt: String = "",
    val updatedByDeviceId: String = "",
    val attachmentManifestVersion: Long = 0
) {
    fun toJson(): String {
        return JSONObject()
            .put("protocolVersion", protocolVersion)
            .put("latestDbSnapshotId", latestDbSnapshotId)
            .put("latestDbPath", latestDbPath)
            .put("latestDbHash", latestDbHash)
            .put("updatedAt", updatedAt)
            .put("updatedByDeviceId", updatedByDeviceId)
            .put("attachmentManifestVersion", attachmentManifestVersion)
            .toString()
    }

    companion object {
        fun fromJson(value: String): SyncManifest {
            val json = JSONObject(value)
            return SyncManifest(
                protocolVersion = json.optInt("protocolVersion", 1),
                latestDbSnapshotId = json.optString("latestDbSnapshotId"),
                latestDbPath = json.optString("latestDbPath"),
                latestDbHash = json.optString("latestDbHash"),
                updatedAt = json.optString("updatedAt"),
                updatedByDeviceId = json.optString("updatedByDeviceId"),
                attachmentManifestVersion = json.optLong("attachmentManifestVersion", 0)
            )
        }
    }
}

data class DatabaseSnapshotMeta(
    val snapshotId: String,
    val createdAt: String,
    val deviceId: String,
    val sha256: String,
    val sizeBytes: Long,
    val roomSchemaVersion: Int
) {
    fun toJson(): String {
        return JSONObject()
            .put("snapshotId", snapshotId)
            .put("createdAt", createdAt)
            .put("deviceId", deviceId)
            .put("sha256", sha256)
            .put("sizeBytes", sizeBytes)
            .put("roomSchemaVersion", roomSchemaVersion)
            .toString()
    }
}

data class AttachmentSyncEntry(
    val path: String,
    val sha256: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val updatedAt: String,
    val updatedByDeviceId: String,
    val deletedAt: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("path", path)
            .put("sha256", sha256)
            .put("sizeBytes", sizeBytes)
            .put("modifiedAt", modifiedAt)
            .put("updatedAt", updatedAt)
            .put("updatedByDeviceId", updatedByDeviceId)
            .put("deletedAt", deletedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): AttachmentSyncEntry {
            return AttachmentSyncEntry(
                path = json.optString("path"),
                sha256 = json.optString("sha256"),
                sizeBytes = json.optLong("sizeBytes"),
                modifiedAt = json.optLong("modifiedAt"),
                updatedAt = json.optString("updatedAt"),
                updatedByDeviceId = json.optString("updatedByDeviceId"),
                deletedAt = json.optString("deletedAt").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }
}

data class AttachmentSyncManifest(
    val version: Long = 0,
    val updatedAt: String = "",
    val files: List<AttachmentSyncEntry> = emptyList()
) {
    fun toJson(): String {
        val fileArray = JSONArray()
        files.forEach { fileArray.put(it.toJson()) }
        return JSONObject()
            .put("version", version)
            .put("updatedAt", updatedAt)
            .put("files", fileArray)
            .toString()
    }

    companion object {
        fun fromJson(value: String): AttachmentSyncManifest {
            val json = JSONObject(value)
            val files = json.optJSONArray("files") ?: JSONArray()
            return AttachmentSyncManifest(
                version = json.optLong("version", 0),
                updatedAt = json.optString("updatedAt"),
                files = buildList {
                    for (index in 0 until files.length()) {
                        add(AttachmentSyncEntry.fromJson(files.getJSONObject(index)))
                    }
                }
            )
        }
    }
}

data class SyncRunResult(
    val message: String,
    val databaseUploaded: Boolean = false,
    val databaseDownloaded: Boolean = false,
    val databaseMerged: Boolean = false,
    val attachmentsUploaded: Int = 0,
    val attachmentsDownloaded: Int = 0
)
