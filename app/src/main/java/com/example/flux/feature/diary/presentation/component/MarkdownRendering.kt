package com.example.flux.feature.diary.presentation.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.flux.R
import com.example.flux.core.util.AttachmentOpener

@Composable
fun MarkdownInlineText(
    text: String,
    highlightQuery: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val openLinkFailedMessage = stringResource(R.string.markdown_open_link_failed)
    val annotated = remember(text, highlightQuery, colors) {
        buildMarkdownAnnotatedString(
            text = text,
            highlightQuery = highlightQuery,
            linkColor = colors.primary,
            inlineCodeBackground = colors.surfaceVariant,
            highlightColor = colors.tertiary.copy(alpha = 0.28f)
        )
    }
    var layoutResult by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(annotated) {
                detectTapGestures { position ->
                    val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                    annotated.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            if (!openLinkTarget(context, annotation.item, uriHandler)) {
                                Toast.makeText(context, openLinkFailedMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
        style = style,
        color = colors.onSurface,
        onTextLayout = { layoutResult = it }
    )
}

@Composable
fun QuoteBlock(
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
        horizontalArrangement = Arrangement.spacedBy(MarkdownDefaults.QuoteSpacing)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                .padding(horizontal = MarkdownDefaults.QuoteIndicatorPadding)
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
fun MarkdownListEntry(
    block: MarkdownBlock.ListEntry,
    highlightQuery: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(MarkdownDefaults.ListSpacing)) {
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
fun CodeBlock(text: String) {
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
fun MarkdownImage(
    altText: String,
    path: String
) {
    val context = LocalContext.current
    val openAttachmentFailedMessage = stringResource(R.string.markdown_open_attachment_failed)
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
                openAttachmentTarget(context, path, openAttachmentFailedMessage)
            }
    )
}

@Composable
fun MarkdownAttachmentChip(
    kind: String,
    label: String,
    path: String
) {
    val context = LocalContext.current
    val openAttachmentFailedMessage = stringResource(R.string.markdown_open_attachment_failed)
    val localizedKind = when (kind.lowercase()) {
        "audio" -> stringResource(R.string.markdown_attachment_audio)
        else -> stringResource(R.string.markdown_attachment_file)
    }
    AssistChip(
        onClick = {
            openAttachmentTarget(context, path, openAttachmentFailedMessage)
        },
        label = { Text("$localizedKind: $label") }
    )
}

private fun openAttachmentTarget(
    context: Context,
    path: String,
    failureMessage: String
) {
    if (!AttachmentOpener.open(context, path)) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

fun buildMarkdownAnnotatedString(
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

fun appendHighlighted(
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

fun openLinkTarget(
    context: Context,
    target: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
): Boolean {
    return when {
        target.contains("attachments/") -> AttachmentOpener.open(context, target)
        else -> runCatching { uriHandler.openUri(target) }.isSuccess
    }
}
