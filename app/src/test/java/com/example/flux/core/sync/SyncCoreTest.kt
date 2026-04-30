package com.example.flux.core.sync

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoreTest {

    private val jianguoyunRoot = "\u6211\u7684\u575a\u679c\u4e91"

    @Test
    fun jianguoyunConfig_addsRootDirectoryForPlainRemoteDir() {
        val path = RemoteSyncPath.from(
            WebDavSyncConfig(
                baseUrl = JIANGUOYUN_WEBDAV_URL,
                username = "user@example.com",
                password = "password",
                remoteDir = "FluxSync"
            )
        )

        assertEquals("$jianguoyunRoot/FluxSync", path.directory)
        assertEquals("$jianguoyunRoot/FluxSync/FluxSync", path.root)
        assertEquals("FluxSync", path.filePrefix)
        assertEquals(1, path.protectedDirectorySegments)
    }

    @Test
    fun jianguoyunConfig_doesNotDuplicateRootDirectory() {
        val path = RemoteSyncPath.from(
            WebDavSyncConfig(
                baseUrl = JIANGUOYUN_WEBDAV_URL,
                username = "user@example.com",
                password = "password",
                remoteDir = "$jianguoyunRoot/FluxSync"
            )
        )

        assertEquals("$jianguoyunRoot/FluxSync", path.directory)
        assertEquals("FluxSync", path.filePrefix)
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
        assertTrue(WebDavSyncConfig(username = "user", password = "password", remoteDir = "FluxSync").isConfigured)
    }

    @Test
    fun manifest_roundTripsAllFields() {
        val manifest = SyncManifest(
            protocolVersion = 2,
            latestDbSnapshotId = "snapshot-1",
            latestDbPath = "db/history/snapshot-1.zip",
            latestDbHash = "abc123",
            updatedAt = "2026-04-30T21:00:00",
            updatedByDeviceId = "device-a",
            attachmentManifestVersion = 42
        )

        val parsed = SyncManifest.fromJson(manifest.toJson())

        assertEquals(manifest, parsed)
    }

    @Test
    fun attachmentManifest_roundTripsDeletedEntries() {
        val manifest = AttachmentSyncManifest(
            version = 3,
            updatedAt = "2026-04-30T21:01:00",
            files = listOf(
                AttachmentSyncEntry(
                    path = "attachments/diaries/a.png",
                    sha256 = "hash-a",
                    sizeBytes = 128,
                    modifiedAt = 1000,
                    updatedAt = "2026-04-30T21:00:00",
                    updatedByDeviceId = "device-a"
                ),
                AttachmentSyncEntry(
                    path = "attachments/diaries/deleted.png",
                    sha256 = "hash-deleted",
                    sizeBytes = 256,
                    modifiedAt = 2000,
                    updatedAt = "2026-04-30T21:02:00",
                    updatedByDeviceId = "device-b",
                    deletedAt = "2026-04-30T21:03:00"
                )
            )
        )

        val parsed = AttachmentSyncManifest.fromJson(manifest.toJson())

        assertEquals(manifest, parsed)
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
