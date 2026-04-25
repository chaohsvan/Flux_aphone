package com.example.flux.feature.diary.presentation.component

import androidx.compose.ui.unit.dp
import com.example.flux.core.util.DataPaths
import java.io.File

sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class ListEntry(
        val text: String,
        val orderedPrefix: String? = null,
        val taskState: Boolean? = null
    ) : MarkdownBlock

    data class CodeBlock(val text: String) : MarkdownBlock
    data class Image(val altText: String, val path: String) : MarkdownBlock
    data class Attachment(val kind: String, val label: String, val path: String) : MarkdownBlock
    data object Divider : MarkdownBlock
    data object Blank : MarkdownBlock
}

object MarkdownDefaults {
    val BlockSpacing = 10.dp
    val BlankSpacing = 2.dp
    val QuoteSpacing = 10.dp
    val QuoteIndicatorPadding = 2.dp
    val ListSpacing = 8.dp
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    if (text.isBlank()) return listOf(MarkdownBlock.Paragraph(""))

    val lines = text.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()
    var index = 0

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphBuffer.joinToString("\n").trimEnd())
            paragraphBuffer.clear()
        }
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            index += 1
            val codeLines = mutableListOf<String>()
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeLines += lines[index]
                index += 1
            }
            blocks += MarkdownBlock.CodeBlock(codeLines.joinToString("\n"))
            index += 1
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            blocks += MarkdownBlock.Blank
            index += 1
            continue
        }

        val imageMatch = IMAGE_PATTERN.matchEntire(trimmed)
        if (imageMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Image(
                altText = imageMatch.groupValues[1],
                path = imageMatch.groupValues[2]
            )
            index += 1
            continue
        }

        val attachmentMatch = ATTACHMENT_PATTERN.matchEntire(trimmed)
        if (attachmentMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Attachment(
                kind = attachmentMatch.groupValues[1],
                label = attachmentMatch.groupValues[2].ifBlank {
                    attachmentMatch.groupValues[3].substringAfterLast('/')
                },
                path = attachmentMatch.groupValues[3]
            )
            index += 1
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Heading(
                level = headingMatch.groupValues[1].length,
                text = headingMatch.groupValues[2]
            )
            index += 1
            continue
        }

        val quoteMatch = Regex("^>\\s?(.*)$").matchEntire(trimmed)
        if (quoteMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Quote(quoteMatch.groupValues[1])
            index += 1
            continue
        }

        val taskMatch = Regex("^[-*+]\\s+\\[( |x|X)]\\s+(.+)$").matchEntire(trimmed)
        if (taskMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.ListEntry(
                text = taskMatch.groupValues[2],
                taskState = taskMatch.groupValues[1].equals("x", ignoreCase = true)
            )
            index += 1
            continue
        }

        val orderedMatch = Regex("^(\\d+\\.)\\s+(.+)$").matchEntire(trimmed)
        if (orderedMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.ListEntry(
                text = orderedMatch.groupValues[2],
                orderedPrefix = orderedMatch.groupValues[1]
            )
            index += 1
            continue
        }

        val bulletMatch = Regex("^[-*+]\\s+(.+)$").matchEntire(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.ListEntry(text = bulletMatch.groupValues[1])
            index += 1
            continue
        }

        if (trimmed == "---" || trimmed == "***") {
            flushParagraph()
            blocks += MarkdownBlock.Divider
            index += 1
            continue
        }

        paragraphBuffer += line
        index += 1
    }

    flushParagraph()
    return blocks
}

fun resolveAttachmentPath(context: android.content.Context, path: String): Any {
    if (!path.contains("attachments/")) return path

    val normalized = DataPaths.normalizeAttachmentPath(path)
    val localFile = DataPaths.attachmentFile(context, normalized)
    if (localFile.exists()) return localFile

    val legacyFile = File(context.filesDir, "assets/$normalized")
    if (legacyFile.exists()) return legacyFile

    val assetPath = normalized.removePrefix("/")
    return "file:///android_asset/$assetPath"
}

const val LINK_TAG = "markdown_link"
val IMAGE_PATTERN = Regex("!\\[(.*?)]\\((.*?)\\)")
val ATTACHMENT_PATTERN = Regex("\\[(audio|file):(.*?)]\\((.*?)\\)")
val INLINE_PATTERN = Regex("(\\*\\*[^*]+\\*\\*)|(\\*[^*]+\\*)|(`[^`]+`)|\\[([^\\]]+)]\\(([^)]+)\\)")
