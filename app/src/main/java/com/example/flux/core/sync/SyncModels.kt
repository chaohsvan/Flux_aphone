package com.example.flux.core.sync

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
