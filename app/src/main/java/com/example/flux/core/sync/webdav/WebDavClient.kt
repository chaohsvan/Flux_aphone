package com.example.flux.core.sync.webdav

import android.util.Base64
import com.example.flux.core.sync.WebDavSyncConfig
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class WebDavClient(
    private val config: WebDavSyncConfig
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    fun testConnection(): Boolean {
        return execute(method = "PROPFIND", path = "", depth = "0", collection = true).use { response ->
            val code = response.code
            if (code in 200..299 || code == 207) true else throw httpError("connect", "", code)
        }
    }

    fun testWrite(path: String) {
        writeText(path, """{"ok":true}""")
        delete(path)
    }

    fun ensureDirectory(path: String, skipLeadingSegments: Int = 0) {
        val parts = cleanPath(path).split('/').filter { it.isNotBlank() }
        var current = ""
        parts.forEachIndexed { index, part ->
            current = if (current.isBlank()) part else "$current/$part"
            if (index < skipLeadingSegments) return@forEachIndexed
            if (exists(current)) return@forEachIndexed
            execute(method = "MKCOL", path = current, collection = true).use { response ->
                val code = response.code
                if (code in 200..299 || code == 405) return@forEachIndexed
                if (code == 403 && exists(current)) return@forEachIndexed
                throw httpError("create directory", current, code)
            }
        }
    }

    fun exists(path: String): Boolean {
        return execute(method = "PROPFIND", path = path, depth = "0", collection = true).use { response ->
            when (val code = response.code) {
                in 200..299, 207 -> true
                404 -> false
                else -> throw httpError("check exists", path, code)
            }
        }
    }

    fun readText(path: String): String? {
        return execute(method = "GET", path = path).use { response ->
            when (val code = response.code) {
                in 200..299 -> response.body?.string().orEmpty()
                404 -> null
                else -> throw httpError("read", path, code)
            }
        }
    }

    fun writeText(path: String, value: String) {
        val body = value.toRequestBody(JSON_MEDIA_TYPE)
        execute(method = "PUT", path = path, body = body).use { response ->
            val code = response.code
            if (code !in 200..299) throw httpError("write", path, code)
        }
    }

    fun uploadFile(path: String, file: File) {
        val body = file.asRequestBody(BINARY_MEDIA_TYPE)
        execute(method = "PUT", path = path, body = body).use { response ->
            val code = response.code
            if (code !in 200..299) throw httpError("upload", path, code)
        }
    }

    fun downloadFile(path: String, target: File) {
        execute(method = "GET", path = path).use { response ->
            val code = response.code
            if (code !in 200..299) throw httpError("download", path, code)
            target.parentFile?.mkdirs()
            response.body?.byteStream()?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    fun delete(path: String) {
        execute(method = "DELETE", path = path).use { response ->
            val code = response.code
            if (code !in 200..299 && code != 404) throw httpError("delete", path, code)
        }
    }

    fun listFiles(path: String): List<String> {
        return execute(method = "PROPFIND", path = path, depth = "1", collection = true).use { response ->
            val code = response.code
            if (code !in 200..299 && code != 207) throw httpError("list", path, code)
            val body = response.body?.string().orEmpty()
            val base = URLDecoder.decode(URL(remoteUrl(path, collection = true)).path, "UTF-8").trim('/')
            Regex("<[^>]*href[^>]*>(.*?)</[^>]*href>", RegexOption.IGNORE_CASE)
                .findAll(body)
                .map { match -> URLDecoder.decode(match.groupValues[1], "UTF-8").trim('/') }
                .mapNotNull { href ->
                    val normalized = href.replace('\\', '/')
                    if (normalized == base) {
                        null
                    } else if (normalized.startsWith("$base/")) {
                        normalized.removePrefix("$base/").trim('/')
                    } else {
                        null
                    }
                }
                .filter { it != base && it.substringAfterLast('/').contains('.') }
                .distinct()
                .toList()
        }
    }

    private fun execute(
        method: String,
        path: String,
        depth: String? = null,
        collection: Boolean = false,
        body: okhttp3.RequestBody? = null
    ): okhttp3.Response {
        val request = Request.Builder()
            .url(remoteUrl(path, collection))
            .header("Authorization", basicAuthorization())
            .apply { depth?.let { header("Depth", it) } }
            .method(method, body)
            .build()
        return client.newCall(request).execute()
    }

    private fun remoteUrl(path: String, collection: Boolean): String {
        val base = config.baseUrl.trimEnd('/')
        val clean = cleanPath(path)
        val encoded = encodePath(clean)
        val raw = if (encoded.isBlank()) "$base/" else "$base/$encoded"
        return if (collection && !raw.endsWith('/')) "$raw/" else raw
    }

    private fun encodePath(path: String): String {
        if (path.isBlank()) return ""
        return path.split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
            }
    }

    private fun basicAuthorization(): String {
        val raw = "${config.username}:${config.password}"
        return "Basic ${Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"
    }

    private fun cleanPath(path: String): String {
        return path.trim().replace('\\', '/').trim('/')
    }

    private fun httpError(operation: String, path: String, statusCode: Int): WebDavHttpException {
        return WebDavHttpException(operation, path, statusCode)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val READ_TIMEOUT_MS = 30_000L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val BINARY_MEDIA_TYPE = "application/octet-stream".toMediaType()
    }
}

class WebDavHttpException(
    val operation: String,
    val path: String,
    val statusCode: Int
) : IllegalStateException("WebDAV $operation failed for $path: HTTP $statusCode")
