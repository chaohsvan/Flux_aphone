package com.example.flux.feature.diary.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.util.DataPaths
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.diary.domain.DiaryFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryEditorUiState(
    val id: String? = null,
    val title: String = "",
    val contentMd: String = "",
    val entryDate: String = "",
    val entryTime: String? = null,
    val mood: String? = null,
    val weather: String? = null,
    val locationName: String? = null,
    val isFavorite: Boolean = false,
    val tagText: String = "",
    val createdAt: String? = null,
    val version: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DiaryEditorViewModel @Inject constructor(
    private val diaryGateway: DiaryFeatureGateway,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val diaryId: String? = savedStateHandle["diaryId"]
    private val initialDate: String? = savedStateHandle["entryDate"]

    private val _uiState = MutableStateFlow(DiaryEditorUiState(isLoading = true))
    val uiState: StateFlow<DiaryEditorUiState> = _uiState.asStateFlow()

    init {
        if (diaryId != null && diaryId != "new") {
            loadDiary(diaryId)
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entryDate = initialDate?.takeIf { date -> date.isNotBlank() } ?: TimeUtil.getCurrentDate()
                )
            }
        }
    }

    private fun loadDiary(id: String) {
        viewModelScope.launch {
            val diary = diaryGateway.getDiaryById(id)
            if (diary != null) {
                val tags = diaryGateway.getTagsForDiary(id).joinToString(", ") { it.name }
                _uiState.update {
                    it.copy(
                        id = diary.id,
                        title = diary.title,
                        contentMd = diary.contentMd,
                        entryDate = diary.entryDate,
                        entryTime = diary.entryTime,
                        mood = diary.mood,
                        weather = diary.weather,
                        locationName = diary.locationName,
                        isFavorite = diary.isFavorite == 1,
                        tagText = tags,
                        createdAt = diary.createdAt,
                        version = diary.version,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(contentMd = newContent) }
    }

    fun updateEntryDate(newDate: String) {
        _uiState.update { it.copy(entryDate = newDate, errorMessage = null) }
    }

    fun updateEntryTime(newTime: String) {
        _uiState.update { it.copy(entryTime = newTime.ifBlank { null }) }
    }

    fun updateMood(newMood: String?) {
        _uiState.update { it.copy(mood = newMood) }
    }

    fun updateWeather(newWeather: String) {
        _uiState.update { it.copy(weather = newWeather.ifBlank { null }) }
    }

    fun updateLocation(newLocation: String) {
        _uiState.update { it.copy(locationName = newLocation.ifBlank { null }) }
    }

    fun updateTagText(newTagText: String) {
        _uiState.update { it.copy(tagText = newTagText) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun attachFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val originalName = queryFileName(context, uri)
            val extension = resolveExtension(originalName, mimeType)
            val knownSize = queryFileSize(context, uri)

            if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
                _uiState.update { it.copy(errorMessage = "不支持的附件类型") }
                return@launch
            }
            if (knownSize == 0L) {
                _uiState.update { it.copy(errorMessage = "附件不能为空") }
                return@launch
            }
            if (knownSize != null && knownSize >= MAX_ATTACHMENT_BYTES) {
                _uiState.update { it.copy(errorMessage = "附件必须小于 100MB") }
                return@launch
            }

            val date = _uiState.value.entryDate.ifBlank { TimeUtil.getCurrentDate() }
            val monthPath = date.take(7).replace("-", "/")
            val relativePath = "attachments/diaries/$monthPath/${TimeUtil.generateUuid()}.$extension"
            val targetFile = DataPaths.attachmentFile(context, relativePath)

            targetFile.parentFile?.mkdirs()
            val copyResult = resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    copyAttachment(input, output)
                }
            }

            when (copyResult) {
                null -> {
                    _uiState.update { it.copy(errorMessage = "无法读取附件") }
                    return@launch
                }
                AttachmentCopyResult.EMPTY -> {
                    targetFile.delete()
                    _uiState.update { it.copy(errorMessage = "附件不能为空") }
                    return@launch
                }
                AttachmentCopyResult.TOO_LARGE -> {
                    targetFile.delete()
                    _uiState.update { it.copy(errorMessage = "附件必须小于 100MB") }
                    return@launch
                }
                AttachmentCopyResult.OK -> Unit
            }

            val label = originalName.substringBeforeLast('.').ifBlank { "附件" }
            val markdown = when {
                extension.isImageExtension() -> "![${label}]($relativePath)"
                extension.isAudioExtension() -> "[audio:${label}]($relativePath)"
                else -> "[file:${label}]($relativePath)"
            }
            appendMarkdown(markdown)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun saveDiary(onSaved: () -> Unit = {}) {
        val currentState = _uiState.value
        if (currentState.title.isBlank() && currentState.contentMd.isBlank()) {
            _uiState.update { it.copy(errorMessage = "标题和正文不能同时为空") }
            return
        }

        val entryDate = currentState.entryDate.ifBlank { TimeUtil.getCurrentDate() }
        if (!TimeUtil.isValidDate(entryDate)) {
            _uiState.update { it.copy(errorMessage = "日期格式应为 YYYY-MM-DD") }
            return
        }

        viewModelScope.launch {
            val existing = diaryGateway.getActiveDiaryByDate(entryDate)
            if (currentState.id != null && existing != null && existing.id != currentState.id) {
                _uiState.update { it.copy(errorMessage = "该日期已经有日记，不能改到这一天") }
                return@launch
            }

            val now = TimeUtil.getCurrentIsoTime()
            val defaultTitle = currentState.contentMd
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.take(24)
                ?: "无标题"
            val entity = DiaryEntity(
                id = currentState.id ?: TimeUtil.generateUuid(),
                entryDate = entryDate,
                entryTime = currentState.entryTime,
                title = currentState.title.ifBlank { defaultTitle },
                contentMd = currentState.contentMd,
                mood = currentState.mood,
                weather = currentState.weather,
                locationName = currentState.locationName,
                isFavorite = if (currentState.isFavorite) 1 else 0,
                wordCount = currentState.contentMd.length,
                createdAt = currentState.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                version = currentState.version + 1,
                restoredAt = null,
                restoredIntoId = null
            )
            diaryGateway.saveDiary(entity, parseTags(currentState.tagText))
            onSaved()
        }
    }

    fun deleteDiary() {
        val id = _uiState.value.id ?: return
        viewModelScope.launch {
            diaryGateway.softDeleteDiary(id)
        }
    }

    private fun parseTags(tagText: String): List<String> {
        return tagText
            .split(Regex("[,;#\\s\\uFF0C\\uFF1B\\u3001]+"))
            .map { it.trim().trimStart('#').take(24) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(12)
    }

    private fun appendMarkdown(markdown: String) {
        _uiState.update { state ->
            val separator = if (state.contentMd.isBlank() || state.contentMd.endsWith("\n")) "" else "\n\n"
            state.copy(contentMd = state.contentMd + separator + markdown)
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index).orEmpty()
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
    }

    private fun queryFileSize(context: Context, uri: Uri): Long? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                return cursor.getLong(index)
            }
        }
        return null
    }

    private fun resolveExtension(fileName: String, mimeType: String): String {
        val fromName = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .take(12)
        if (fromName.isNotBlank()) return fromName
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    }

    private fun copyAttachment(
        input: java.io.InputStream,
        output: java.io.OutputStream
    ): AttachmentCopyResult {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            totalBytes += read
            if (totalBytes >= MAX_ATTACHMENT_BYTES) return AttachmentCopyResult.TOO_LARGE
            output.write(buffer, 0, read)
        }
        return if (totalBytes == 0L) AttachmentCopyResult.EMPTY else AttachmentCopyResult.OK
    }

    private fun String.isImageExtension(): Boolean {
        return this in IMAGE_ATTACHMENT_EXTENSIONS
    }

    private fun String.isAudioExtension(): Boolean {
        return this in AUDIO_ATTACHMENT_EXTENSIONS
    }

    private enum class AttachmentCopyResult {
        OK,
        EMPTY,
        TOO_LARGE
    }

    companion object {
        private const val MAX_ATTACHMENT_BYTES = 100L * 1024L * 1024L
        private val IMAGE_ATTACHMENT_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
        private val AUDIO_ATTACHMENT_EXTENSIONS = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac")
        private val ALLOWED_ATTACHMENT_EXTENSIONS = IMAGE_ATTACHMENT_EXTENSIONS +
            AUDIO_ATTACHMENT_EXTENSIONS +
            setOf(
                "pdf",
                "txt",
                "md",
                "csv",
                "json",
                "doc",
                "docx",
                "xls",
                "xlsx",
                "ppt",
                "pptx",
                "zip",
                "rar",
                "7z"
            )
    }

}
