package com.example.flux.core.sync

import java.net.URLDecoder

data class RemoteSyncPath(
    val directory: String,
    val filePrefix: String,
    val protectedDirectorySegments: Int
) {
    val root: String = "$directory/$filePrefix"

    companion object {
        fun from(config: WebDavSyncConfig): RemoteSyncPath {
            val configuredDirectory = config.remoteDir
                .trim()
                .replace('\\', '/')
                .trim('/')
                .ifBlank { DEFAULT_DIRECTORY }
            val decodedBaseUrl = runCatching {
                URLDecoder.decode(config.baseUrl, Charsets.UTF_8.name())
            }.getOrDefault(config.baseUrl)
            val isJianguoyun = decodedBaseUrl.contains("jianguoyun.com", ignoreCase = true)
            val baseUrlIncludesJianguoyunRoot = isJianguoyun &&
                decodedBaseUrl.contains(JIANGUOYUN_ROOT, ignoreCase = false)
            val directory = if (
                isJianguoyun &&
                !baseUrlIncludesJianguoyunRoot &&
                !configuredDirectory.startsWith(JIANGUOYUN_ROOT)
            ) {
                "$JIANGUOYUN_ROOT/$configuredDirectory"
            } else {
                configuredDirectory
            }
            val filePrefix = directory.substringAfterLast('/').ifBlank { DEFAULT_DIRECTORY }
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val protectedSegments = if (
                isJianguoyun &&
                !baseUrlIncludesJianguoyunRoot &&
                directory.startsWith("$JIANGUOYUN_ROOT/")
            ) {
                1
            } else {
                0
            }
            return RemoteSyncPath(
                directory = directory,
                filePrefix = filePrefix,
                protectedDirectorySegments = protectedSegments
            )
        }

        private const val DEFAULT_DIRECTORY = "FluxSync"
        private const val JIANGUOYUN_ROOT = "\u6211\u7684\u575a\u679c\u4e91"
    }
}
