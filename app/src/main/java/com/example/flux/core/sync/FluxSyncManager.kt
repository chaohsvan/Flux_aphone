package com.example.flux.core.sync

import android.content.Context
import android.util.Base64
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.sync.webdav.WebDavClient
import com.example.flux.core.sync.webdav.WebDavHttpException
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class FluxSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateStore: SyncStateStore,
    private val snapshotManager: DatabaseSnapshotManager,
    private val attachmentScanner: LocalAttachmentScanner
) {
    suspend fun testConnection(config: WebDavSyncConfig): Boolean = withContext(Dispatchers.IO) {
        require(config.isConfigured) { "同步配置不完整" }
        val client = WebDavClient(config)
        val remotePath = RemoteSyncPath.from(config)
        val testPath = "${remotePath.root}_connection_test.json"
        runCatching {
            ensureRemoteDirectory(client, remotePath)
            client.testWrite(testPath)
        }.getOrElse { throwable ->
            throw IllegalStateException(webDavFailureMessage(remotePath.directory, throwable))
        }
        true
    }

    suspend fun syncNow(): SyncRunResult = withContext(Dispatchers.IO) {
        val config = stateStore.getConfig()
        require(config.enabled) { "Sync is not enabled" }
        require(config.isConfigured) { "Sync configuration is incomplete" }
        if (stateStore.getStatus().isRunning) {
            return@withContext SyncRunResult("同步已在进行中")
        }

        stateStore.markRunning(true)
        val result = runCatching {
            stateStore.appendLog("INFO", "开始同步")
            val result = runSync(config)
            stateStore.markSuccess(result.message)
            result
        }.onFailure { throwable ->
            stateStore.markError(syncFailureMessage(config, throwable))
        }
        result.getOrElse { throwable ->
            throw IllegalStateException(syncFailureMessage(config, throwable))
        }
    }

    private suspend fun runSync(config: WebDavSyncConfig): SyncRunResult {
        val deviceId = stateStore.getDeviceId()
        val client = WebDavClient(config)
        val remotePath = RemoteSyncPath.from(config)
        val root = remotePath.root
        ensureRemoteDirectory(client, remotePath)

        val remoteManifest = client.readText(remoteManifestPath(root))?.let(SyncManifest::fromJson)
        val localDbHash = snapshotManager.currentDatabaseHash(context)
        val lastLocalDbHash = stateStore.getLastLocalDbHash()
        val lastRemoteSnapshotId = stateStore.getLastRemoteSnapshotId()
        val localChanged = lastLocalDbHash.isBlank() || localDbHash != lastLocalDbHash
        val remoteChanged = remoteManifest != null && remoteManifest.latestDbSnapshotId != lastRemoteSnapshotId

        var manifest = remoteManifest ?: SyncManifest(updatedAt = TimeUtil.getCurrentIsoTime())
        var databaseUploaded = false
        var databaseDownloaded = false
        var databaseMerged = false

        when {
            localChanged && remoteChanged -> {
                downloadDatabaseSnapshot(client, root, remoteManifest, ImportBackupMode.Merge)
                databaseMerged = true
                manifest = uploadDatabaseSnapshot(client, root, deviceId, remoteManifest)
                databaseUploaded = true
            }
            localChanged -> {
                manifest = uploadDatabaseSnapshot(client, root, deviceId, manifest)
                databaseUploaded = true
            }
            remoteManifest == null -> {
                manifest = uploadDatabaseSnapshot(client, root, deviceId, manifest)
                databaseUploaded = true
            }
            remoteChanged -> {
                downloadDatabaseSnapshot(client, root, remoteManifest, ImportBackupMode.Replace)
                databaseDownloaded = true
            }
        }

        val attachmentResult = syncAttachments(client, root, deviceId)
        val finalDbHash = snapshotManager.currentDatabaseHash(context)
        stateStore.updateDatabaseSyncState(
            localDbHash = finalDbHash,
            remoteSnapshotId = manifest.latestDbSnapshotId
        )

        val message = buildString {
            append("同步完成")
            if (databaseMerged) append("，已合并云端数据库")
            else if (databaseUploaded) append("，已上传数据库")
            else if (databaseDownloaded) append("，已下载数据库")
            append("，上传附件 ${attachmentResult.uploaded}")
            append("，下载附件 ${attachmentResult.downloaded}")
        }
        return SyncRunResult(
            message = message,
            databaseUploaded = databaseUploaded,
            databaseDownloaded = databaseDownloaded,
            databaseMerged = databaseMerged,
            attachmentsUploaded = attachmentResult.uploaded,
            attachmentsDownloaded = attachmentResult.downloaded
        )
    }

    private fun uploadDatabaseSnapshot(
        client: WebDavClient,
        root: String,
        deviceId: String,
        previousManifest: SyncManifest
    ): SyncManifest {
        val snapshot = snapshotManager.createSnapshot(context, deviceId)
        val historyPath = "${root}_db_history_${snapshot.meta.snapshotId}.zip"
        val latestPath = "${root}_db_latest.zip"
        client.uploadFile(historyPath, snapshot.file)
        client.uploadFile(latestPath, snapshot.file)
        client.writeText("${root}_db_latest.meta.json", snapshot.meta.toJson())
        pruneDatabaseHistory(client, root)

        val manifest = previousManifest.copy(
            latestDbSnapshotId = snapshot.meta.snapshotId,
            latestDbPath = latestPath,
            latestDbHash = snapshot.meta.sha256,
            updatedAt = snapshot.meta.createdAt,
            updatedByDeviceId = deviceId
        )
        client.writeText(remoteManifestPath(root), manifest.toJson())
        snapshot.file.delete()
        return manifest
    }

    private fun pruneDatabaseHistory(client: WebDavClient, root: String) {
        runCatching {
            val directory = root.substringBeforeLast('/')
            val filePrefix = root.substringAfterLast('/')
            client.listFiles(directory)
                .filter { it.endsWith(".zip") }
                .filter { it.substringAfterLast('/').startsWith("${filePrefix}_db_history_") }
                .sortedDescending()
                .drop(HISTORY_KEEP_COUNT)
                .forEach { client.delete("$directory/${it.trim('/')}") }
        }.onFailure { throwable ->
            stateStore.appendLog("WARN", "历史快照清理失败：${throwable.message ?: throwable::class.java.simpleName}")
        }
    }

    private suspend fun downloadDatabaseSnapshot(
        client: WebDavClient,
        root: String,
        manifest: SyncManifest,
        mode: ImportBackupMode
    ) {
        val latestPath = if (manifest.latestDbPath.isBlank()) "${root}_db_latest.zip" else manifest.latestDbPath
        val historyPath = "${root}_db_history_${manifest.latestDbSnapshotId}.zip"
        val candidates = listOf(latestPath, historyPath)
            .filter { it.isNotBlank() }
            .distinct()
        val target = File(context.cacheDir, "flux_remote_${TimeUtil.generateUuid()}.zip")
        try {
            var lastMismatch: SnapshotChecksumMismatchException? = null
            var imported = false
            candidates.forEach { remotePath ->
                if (imported) return@forEach
                client.downloadFile(remotePath, target)
                val actualHash = HashUtil.sha256(target)
                if (manifest.latestDbHash.isNotBlank() && actualHash != manifest.latestDbHash) {
                    lastMismatch = SnapshotChecksumMismatchException(
                        path = remotePath,
                        expected = manifest.latestDbHash,
                        actual = actualHash
                    )
                    stateStore.appendLog("WARN", "远端数据库快照校验失败，尝试备用文件：$remotePath")
                    target.delete()
                } else {
                    snapshotManager.importSnapshot(context, target, mode)
                    imported = true
                }
            }
            if (!imported) {
                throw lastMismatch ?: IllegalStateException("Remote database snapshot is unavailable")
            }
        } finally {
            target.delete()
        }
    }

    private suspend fun syncAttachments(client: WebDavClient, root: String, deviceId: String): AttachmentResult {
        val remoteManifestPath = "${root}_attachments_manifest.json"
        val remoteManifest = client.readText(remoteManifestPath)
            ?.let(AttachmentSyncManifest::fromJson)
            ?: AttachmentSyncManifest()
        val remoteByPath = remoteManifest.files.associateBy { it.path }
        val localFiles = attachmentScanner.scan(context, deviceId)
        val localByPath = localFiles.associateBy { it.path }
        val previouslySyncedPaths = stateStore.getLastAttachmentPaths()

        var uploaded = 0
        var downloaded = 0
        val merged = remoteByPath.toMutableMap()
        val uploadedPaths = mutableSetOf<String>()

        localFiles.forEach { local ->
            val remote = remoteByPath[local.path]
            if (remote?.deletedAt != null) {
                val file = attachmentScanner.fileForPath(context, local.path)
                if (file.exists()) file.delete()
                merged[local.path] = remote
            } else if (remote == null || remote.sha256 != local.sha256) {
                uploadAttachment(client, root, local.path)
                merged[local.path] = local.copy(updatedAt = TimeUtil.getCurrentIsoTime(), updatedByDeviceId = deviceId)
                uploadedPaths += local.path
                uploaded += 1
            }
        }

        remoteManifest.files
            .filter {
                it.path !in uploadedPaths &&
                    it.deletedAt == null &&
                    it.path !in previouslySyncedPaths &&
                    localByPath[it.path]?.sha256 != it.sha256
            }
            .forEach { remote ->
                downloadAttachment(client, root, remote.path)
                merged[remote.path] = remote
                downloaded += 1
            }

        previouslySyncedPaths
            .filter { path -> path !in localByPath && remoteByPath[path]?.deletedAt == null }
            .forEach { deletedPath ->
                val remote = remoteByPath[deletedPath]
                merged[deletedPath] = AttachmentSyncEntry(
                    path = deletedPath,
                    sha256 = remote?.sha256.orEmpty(),
                    sizeBytes = remote?.sizeBytes ?: 0L,
                    modifiedAt = remote?.modifiedAt ?: 0L,
                    updatedAt = TimeUtil.getCurrentIsoTime(),
                    updatedByDeviceId = deviceId,
                    deletedAt = TimeUtil.getCurrentIsoTime()
                )
                runCatching { client.delete(remoteAttachmentPath(root, deletedPath)) }
                    .onFailure { throwable ->
                        stateStore.appendLog("WARN", "远端附件删除失败：$deletedPath ${throwable.message.orEmpty()}")
                    }
            }

        val nextManifest = AttachmentSyncManifest(
            version = remoteManifest.version + 1,
            updatedAt = TimeUtil.getCurrentIsoTime(),
            files = merged.values.sortedBy { it.path }
        )
        client.writeText(remoteManifestPath, nextManifest.toJson())
        stateStore.updateLastAttachmentPaths(
            nextManifest.files.filter { it.deletedAt == null }.map { it.path }.toSet()
        )
        return AttachmentResult(uploaded, downloaded)
    }

    private fun uploadAttachment(client: WebDavClient, root: String, relativePath: String) {
        val file = attachmentScanner.fileForPath(context, relativePath)
        client.uploadFile(remoteAttachmentPath(root, relativePath), file)
    }

    private fun downloadAttachment(client: WebDavClient, root: String, relativePath: String) {
        val target = attachmentScanner.fileForPath(context, relativePath)
        client.downloadFile(remoteAttachmentPath(root, relativePath), target)
    }

    private fun remoteManifestPath(root: String): String = "${root}_manifest.json"

    private fun remoteAttachmentPath(root: String, relativePath: String): String {
        val encoded = Base64.encodeToString(relativePath.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return "${root}_att_$encoded"
    }

    private fun webDavFailureMessage(remoteDir: String, throwable: Throwable): String {
        val checksumMismatch = throwable as? SnapshotChecksumMismatchException
        if (checksumMismatch != null) {
            return "远端数据库快照校验失败，路径：${checksumMismatch.path}"
        }
        val webDavError = throwable as? WebDavHttpException
        if (webDavError != null) {
            return when (webDavError.statusCode) {
                401 -> "WebDAV 账号或应用密码验证失败，请确认使用坚果云第三方应用密码"
                403 -> when (webDavError.operation) {
                    "create directory" -> "WebDAV 无权限创建同步目录：$remoteDir，失败路径：${webDavError.path}"
                    else -> "WebDAV 无权限访问目录：$remoteDir，操作：${webDavError.operation}，路径：${webDavError.path}"
                }
                404 -> "WebDAV 可连接，但路径不存在：${webDavError.path}"
                409 -> "WebDAV 同步目录的父目录不存在或路径冲突：$remoteDir"
                else -> "WebDAV ${webDavError.operation} 失败：HTTP ${webDavError.statusCode}，路径：${webDavError.path}"
            }
        }
        return "WebDAV 操作失败：${throwable.message ?: throwable::class.java.simpleName}"
    }

    private fun ensureRemoteDirectory(client: WebDavClient, remotePath: RemoteSyncPath) {
        client.ensureDirectory(
            path = remotePath.directory,
            skipLeadingSegments = remotePath.protectedDirectorySegments
        )
    }

    private fun syncFailureMessage(config: WebDavSyncConfig, throwable: Throwable): String {
        return webDavFailureMessage(RemoteSyncPath.from(config).directory, throwable)
    }

    private data class AttachmentResult(
        val uploaded: Int,
        val downloaded: Int
    )

    private companion object {
        const val HISTORY_KEEP_COUNT = 10
    }
}

private class SnapshotChecksumMismatchException(
    val path: String,
    val expected: String,
    val actual: String
) : IllegalStateException("Remote database snapshot checksum mismatch at $path")
