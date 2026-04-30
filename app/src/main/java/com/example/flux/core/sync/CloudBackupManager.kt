package com.example.flux.core.sync

import android.content.Context
import com.example.flux.core.domain.settings.ExportBackupUseCase
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.domain.settings.ImportBackupUseCase
import com.example.flux.core.sync.webdav.WebDavClient
import com.example.flux.core.sync.webdav.WebDavHttpException
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CloudBackupResult(
    val message: String,
    val remotePath: String = ""
)

@Singleton
class CloudBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateStore: SyncStateStore,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
) {
    suspend fun backupNow(): CloudBackupResult = withContext(Dispatchers.IO) {
        val config = backupConfig()
        val client = WebDavClient(config)
        val remotePath = RemoteSyncPath.from(config)
        val fileName = "FluxBackup_${TimeUtil.getCurrentIsoTime().toFileNamePart()}.zip"
        val remoteFile = "${remotePath.directory}/$fileName"
        val localFile = File(context.cacheDir, fileName)

        try {
            client.ensureDirectory(
                path = remotePath.directory,
                skipLeadingSegments = remotePath.protectedDirectorySegments
            )
            exportBackupUseCase.toFile(context, localFile)
            client.uploadFile(remoteFile, localFile)
            client.writeText(
                "${remotePath.directory}/latest_backup.json",
                JSONObject()
                    .put("fileName", fileName)
                    .put("remotePath", remoteFile)
                    .put("createdAt", TimeUtil.getCurrentIsoTime())
                    .put("sha256", HashUtil.sha256(localFile))
                    .put("sizeBytes", localFile.length())
                    .toString()
            )
            CloudBackupResult(
                message = "\u5df2\u5907\u4efd\u5230\u4e91\u7aef\uff1a$fileName",
                remotePath = remoteFile
            )
        } catch (throwable: Throwable) {
            throw IllegalStateException(cloudBackupFailureMessage(remotePath.directory, throwable))
        } finally {
            localFile.delete()
        }
    }

    suspend fun restoreLatest(mode: ImportBackupMode): CloudBackupResult = withContext(Dispatchers.IO) {
        val config = backupConfig()
        val client = WebDavClient(config)
        val remotePath = RemoteSyncPath.from(config)
        val target = File(context.cacheDir, "flux_cloud_restore_${TimeUtil.generateUuid()}.zip")

        try {
            val remoteFile = latestBackupPath(client, remotePath.directory)
            client.downloadFile(remoteFile, target)
            importBackupUseCase.fromFile(context, target, mode)
            CloudBackupResult(
                message = if (mode == ImportBackupMode.Merge) {
                    "\u5df2\u4ece\u4e91\u7aef\u589e\u91cf\u6062\u590d\u5907\u4efd"
                } else {
                    "\u5df2\u4ece\u4e91\u7aef\u6062\u590d\u5907\u4efd\uff0c\u8bf7\u91cd\u542f\u5e94\u7528"
                },
                remotePath = remoteFile
            )
        } catch (throwable: Throwable) {
            throw IllegalStateException(cloudBackupFailureMessage(remotePath.directory, throwable))
        } finally {
            target.delete()
        }
    }

    private fun backupConfig(): WebDavSyncConfig {
        val config = stateStore.getConfig()
        require(config.username.isNotBlank() && config.password.isNotBlank()) {
            "\u8bf7\u5148\u5728\u591a\u7aef\u540c\u6b65\u4e2d\u586b\u5199 WebDAV \u8d26\u53f7\u548c\u5e94\u7528\u5bc6\u7801"
        }
        return config.copy(
            enabled = true,
            baseUrl = JIANGUOYUN_WEBDAV_URL,
            remoteDir = CLOUD_BACKUP_DIRECTORY
        )
    }

    private fun latestBackupPath(client: WebDavClient, directory: String): String {
        client.readText("$directory/latest_backup.json")?.let { raw ->
            val path = JSONObject(raw).optString("remotePath")
            if (path.isNotBlank()) return path
        }
        val latestFile = client.listFiles(directory)
            .map { it.trim('/') }
            .filter { it.startsWith("FluxBackup_") && it.endsWith(".zip") }
            .sortedDescending()
            .firstOrNull()
            ?: error("\u4e91\u7aef\u6ca1\u6709\u53ef\u6062\u590d\u7684\u5907\u4efd")
        return "$directory/$latestFile"
    }

    private fun String.toFileNamePart(): String {
        return replace(":", "")
            .replace("-", "")
            .replace(".", "")
    }

    private fun cloudBackupFailureMessage(remoteDir: String, throwable: Throwable): String {
        val webDavError = throwable as? WebDavHttpException
        if (webDavError != null) {
            return when (webDavError.statusCode) {
                401 -> "WebDAV \u8d26\u53f7\u6216\u5e94\u7528\u5bc6\u7801\u9a8c\u8bc1\u5931\u8d25"
                403 -> "WebDAV \u65e0\u6743\u9650\u8bbf\u95ee\u4e91\u5907\u4efd\u76ee\u5f55\uff1a$remoteDir"
                404 -> "\u4e91\u5907\u4efd\u76ee\u5f55\u6216\u6587\u4ef6\u4e0d\u5b58\u5728\uff1a${webDavError.path}"
                else -> "WebDAV \u4e91\u5907\u4efd\u64cd\u4f5c\u5931\u8d25\uff1aHTTP ${webDavError.statusCode}"
            }
        }
        return throwable.message ?: "\u4e91\u5907\u4efd\u64cd\u4f5c\u5931\u8d25"
    }

    private companion object {
        const val CLOUD_BACKUP_DIRECTORY = "FluxBackups"
    }
}
