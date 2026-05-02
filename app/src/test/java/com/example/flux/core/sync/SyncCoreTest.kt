package com.example.flux.core.sync

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoreTest {

    private val jianguoyunRoot = "\u6211\u7684\u575a\u679c\u4e91"

    @Test
    fun jianguoyunConfig_addsRootDirectoryForCloudBackupDir() {
        val path = RemoteSyncPath.from(
            WebDavSyncConfig(
                baseUrl = JIANGUOYUN_WEBDAV_URL,
                username = "user@example.com",
                password = "password",
                remoteDir = "FluxBackups"
            )
        )

        assertEquals("$jianguoyunRoot/FluxBackups", path.directory)
        assertEquals("$jianguoyunRoot/FluxBackups/FluxBackups", path.root)
        assertEquals("FluxBackups", path.filePrefix)
        assertEquals(1, path.protectedDirectorySegments)
    }

    @Test
    fun jianguoyunConfig_doesNotDuplicateRootDirectory() {
        val path = RemoteSyncPath.from(
            WebDavSyncConfig(
                baseUrl = JIANGUOYUN_WEBDAV_URL,
                username = "user@example.com",
                password = "password",
                remoteDir = "$jianguoyunRoot/FluxBackups"
            )
        )

        assertEquals("$jianguoyunRoot/FluxBackups", path.directory)
        assertEquals("FluxBackups", path.filePrefix)
        assertEquals(1, path.protectedDirectorySegments)
    }

    @Test
    fun remotePath_normalizesBackslashesAndUnsafePrefixChars() {
        val path = RemoteSyncPath.from(
            WebDavSyncConfig(
                baseUrl = "https://example.com/dav/",
                username = "user",
                password = "password",
                remoteDir = "\\Team Space\\Flux Sync!"
            )
        )

        assertEquals("Team Space/Flux Sync!", path.directory)
        assertEquals("Flux_Sync_", path.filePrefix)
        assertEquals(0, path.protectedDirectorySegments)
    }

    @Test
    fun config_isConfiguredRequiresCredentialsAndRemoteDir() {
        assertFalse(WebDavSyncConfig(username = "user", password = "").isConfigured)
        assertFalse(WebDavSyncConfig(username = "", password = "password").isConfigured)
        assertFalse(WebDavSyncConfig(username = "user", password = "password", remoteDir = "").isConfigured)
        assertTrue(WebDavSyncConfig(username = "user", password = "password", remoteDir = "FluxBackups").isConfigured)
    }

    @Test
    fun hashUtil_calculatesKnownSha256ForBytesAndFile() {
        val bytes = "Flux sync".toByteArray()
        val expected = "78ad81bface39944bb6f52ed8a5db6a5f7066d9c551af873010ef9c0f6872975"
        val temp = File.createTempFile("flux-sync-hash", ".txt")
        try {
            temp.writeBytes(bytes)

            assertEquals(expected, HashUtil.sha256(bytes))
            assertEquals(expected, HashUtil.sha256(temp))
        } finally {
            temp.delete()
        }
    }
}
