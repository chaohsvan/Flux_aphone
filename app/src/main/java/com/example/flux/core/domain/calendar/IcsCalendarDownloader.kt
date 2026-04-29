package com.example.flux.core.domain.calendar

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class IcsDownloadResult {
    data class Downloaded(
        val body: String,
        val etag: String?,
        val lastModified: String?
    ) : IcsDownloadResult()

    data class NotModified(
        val etag: String?,
        val lastModified: String?
    ) : IcsDownloadResult()
}

class IcsCalendarDownloader @Inject constructor() {
    fun download(url: String, etag: String?, lastModified: String?): IcsDownloadResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/calendar,text/plain,*/*")
            etag?.takeIf { it.isNotBlank() }?.let { setRequestProperty("If-None-Match", it) }
            lastModified?.takeIf { it.isNotBlank() }?.let { setRequestProperty("If-Modified-Since", it) }
        }

        return connection.use {
            when (responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> IcsDownloadResult.NotModified(
                    etag = getHeaderField("ETag") ?: etag,
                    lastModified = getHeaderField("Last-Modified") ?: lastModified
                )
                in 200..299 -> IcsDownloadResult.Downloaded(
                    body = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() },
                    etag = getHeaderField("ETag") ?: etag,
                    lastModified = getHeaderField("Last-Modified") ?: lastModified
                )
                else -> {
                    val message = errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    error("ICS download failed: HTTP $responseCode ${message.take(200)}")
                }
            }
        }
    }

    private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
        return try {
            block()
        } finally {
            disconnect()
        }
    }
}
