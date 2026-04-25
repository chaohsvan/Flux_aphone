package com.example.flux.feature.diary.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    highlightQuery: String = ""
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MarkdownDefaults.BlockSpacing)) {
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
                MarkdownBlock.Blank -> Box(modifier = Modifier.padding(vertical = MarkdownDefaults.BlankSpacing))
            }
        }
    }
}
