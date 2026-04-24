package com.example.flux.feature.diary.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.flux.core.util.AttachmentOpener
import com.example.flux.core.util.DataPaths
import java.io.File

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    highlightQuery: String = ""
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> MarkdownInlineText(
                    text = block.text,
                    highlightQuery = highlightQuery,
                    style = MaterialTheme.typography.bodyLarge
                )

                is MarkdownBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.SemiBold
                )

                is MarkdownBlock.Quote -> QuoteBlock(
                    text = block.text,
                    highlightQuery = highlightQuery
                )

                is MarkdownBlock.ListEntry -> MarkdownListEntry(
                    block = block,
                    highlightQuery = highlightQuery
                )

                is MarkdownBlock.CodeBlock -> CodeBlock(text = block.text)
                is MarkdownBlock.Image -> MarkdownImage(altText = block.altText, path = block.path)
                is MarkdownBlock.Attachment -> MarkdownAttachmentChip(
                    kind = block.kind,
                    label = block.label,
                    path = block.path
                )

                MarkdownBlock.Divider -> HorizontalDivider()
                MarkdownBlock.Blank -> Box(modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun MarkdownInlineText(
    text: String,
    highlightQuery: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val annotated = remember(text, highlightQuery, colors) {
        buildMarkdownAnnotatedString(
            text = text,
            highlightQuery = highlightQuery,
            linkColor = colors.primary,
            inlineCodeBackground = colors.surfaceVariant,
            highlightColor = colors.tertiary.copy(alpha = 0.28f)
        )
    }
    ClickableText(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        style = style.copy(color = colors.onSurface),
        onClick = { offset ->
            annotated
                .getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    if (!openLinkTarget(context, annotation.item, uriHandler)) {
                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    )
}

@Composable
private fun QuoteBlock(
    text: String,
    highlightQuery: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                .padding(horizontal = 2.dp)
        )
        MarkdownInlineText(
            text = text,
            highlightQuery = highlightQuery,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarkdownListEntry(
    block: MarkdownBlock.ListEntry,
    highlightQuery: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = when {
                block.taskState != null -> if (block.taskState) "[x]" else "[ ]"
                block.orderedPrefix != null -> block.orderedPrefix
                else -> "-"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        MarkdownInlineText(
            text = block.text,
            highlightQuery = highlightQuery,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CodeBlock(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    )
}

@Composable
private fun MarkdownImage(
    altText: String,
    path: String
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolveAttachmentPath(context, path))
            .crossfade(true)
            .build(),
        contentDescription = altText,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!AttachmentOpener.open(context, path)) {
                    Toast.makeText(context, "无法打开附件", Toast.LENGTH_SHORT).show()
                }
            }
    )
}

@Composable
private fun MarkdownAttachmentChip(
    kind: String,
    label: String,
    path: String
) {
    val context = LocalContext.current
    AssistChip(
        onClick = {
            if (!AttachmentOpener.open(context, path)) {
                Toast.makeText(context, "无法打开附件", Toast.LENGTH_SHORT).show()
            }
        },
        label = { Text("$kind: $label") }
    )
}

private sealed interface MarkdownBlock {
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

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
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
                kind = if (attachmentMatch.groupValues[1] == "audio") "音频" else "文件",
                label = attachmentMatch.groupValues[2].ifBlank { attachmentMatch.groupValues[3].substringAfterLast('/') },
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

private fun buildMarkdownAnnotatedString(
    text: String,
    highlightQuery: String,
    linkColor: Color,
    inlineCodeBackground: Color,
    highlightColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var lastIndex = 0

    INLINE_PATTERN.findAll(text).forEach { match ->
        appendHighlighted(
            builder = builder,
            text = text.substring(lastIndex, match.range.first),
            highlightQuery = highlightQuery,
            baseStyle = SpanStyle(),
            highlightStyle = SpanStyle(background = highlightColor)
        )

        when {
            match.value.startsWith("**") -> appendHighlighted(
                builder = builder,
                text = match.value.removePrefix("**").removeSuffix("**"),
                highlightQuery = highlightQuery,
                baseStyle = SpanStyle(fontWeight = FontWeight.Bold),
                highlightStyle = SpanStyle(background = highlightColor)
            )

            match.value.startsWith("*") -> appendHighlighted(
                builder = builder,
                text = match.value.removePrefix("*").removeSuffix("*"),
                highlightQuery = highlightQuery,
                baseStyle = SpanStyle(fontStyle = FontStyle.Italic),
                highlightStyle = SpanStyle(background = highlightColor)
            )

            match.value.startsWith("`") -> appendHighlighted(
                builder = builder,
                text = match.value.removePrefix("`").removeSuffix("`"),
                highlightQuery = highlightQuery,
                baseStyle = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = inlineCodeBackground
                ),
                highlightStyle = SpanStyle(background = highlightColor)
            )

            match.groups[4] != null && match.groups[5] != null -> {
                val start = builder.length
                appendHighlighted(
                    builder = builder,
                    text = match.groups[4]!!.value,
                    highlightQuery = highlightQuery,
                    baseStyle = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    highlightStyle = SpanStyle(background = highlightColor)
                )
                builder.addStringAnnotation(
                    tag = LINK_TAG,
                    annotation = match.groups[5]!!.value,
                    start = start,
                    end = builder.length
                )
            }
        }

        lastIndex = match.range.last + 1
    }

    appendHighlighted(
        builder = builder,
        text = text.substring(lastIndex),
        highlightQuery = highlightQuery,
        baseStyle = SpanStyle(),
        highlightStyle = SpanStyle(background = highlightColor)
    )
    return builder.toAnnotatedString()
}

private fun appendHighlighted(
    builder: AnnotatedString.Builder,
    text: String,
    highlightQuery: String,
    baseStyle: SpanStyle,
    highlightStyle: SpanStyle
) {
    if (text.isEmpty()) return
    val query = highlightQuery.trim()
    if (query.isBlank()) {
        builder.pushStyle(baseStyle)
        builder.append(text)
        builder.pop()
        return
    }

    var start = 0
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    while (true) {
        val matchIndex = lowerText.indexOf(lowerQuery, startIndex = start)
        if (matchIndex < 0) {
            builder.pushStyle(baseStyle)
            builder.append(text.substring(start))
            builder.pop()
            return
        }
        if (matchIndex > start) {
            builder.pushStyle(baseStyle)
            builder.append(text.substring(start, matchIndex))
            builder.pop()
        }
        val end = matchIndex + query.length
        builder.pushStyle(baseStyle.merge(highlightStyle))
        builder.append(text.substring(matchIndex, end))
        builder.pop()
        start = end
    }
}

private fun openLinkTarget(
    context: Context,
    target: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
): Boolean {
    return when {
        target.contains("attachments/") -> AttachmentOpener.open(context, target)
        else -> runCatching {
            uriHandler.openUri(target)
        }.isSuccess
    }
}

private fun resolveAttachmentPath(context: Context, path: String): Any {
    if (!path.contains("attachments/")) return path

    val normalized = DataPaths.normalizeAttachmentPath(path)
    val localFile = DataPaths.attachmentFile(context, normalized)
    if (localFile.exists()) return localFile

    val legacyFile = File(context.filesDir, "assets/$normalized")
    if (legacyFile.exists()) return legacyFile

    val assetPath = normalized.removePrefix("/")
    return "file:///android_asset/$assetPath"
}

private const val LINK_TAG = "markdown_link"
private val IMAGE_PATTERN = Regex("!\\[(.*?)]\\((.*?)\\)")
private val ATTACHMENT_PATTERN = Regex("\\[(audio|file):(.*?)]\\((.*?)\\)")
private val INLINE_PATTERN = Regex("(\\*\\*[^*]+\\*\\*)|(\\*[^*]+\\*)|(`[^`]+`)|\\[([^\\]]+)]\\(([^)]+)\\)")
