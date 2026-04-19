package com.example.flux.feature.diary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val regex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
    val matches = regex.findAll(text)

    if (matches.none()) {
        Text(text = text, modifier = modifier, style = MaterialTheme.typography.bodyLarge)
        return
    }

    Column(modifier = modifier) {
        var lastEnd = 0
        matches.forEach { match ->
            val textBefore = text.substring(lastEnd, match.range.first)
            if (textBefore.isNotBlank()) {
                Text(text = textBefore, style = MaterialTheme.typography.bodyLarge)
            }

            val altText = match.groupValues[1]
            val imgPath = match.groupValues[2]

            // 将原有 Markdown 路径映射到 Android assets
            val assetPath = if (imgPath.contains("attachments/")) {
                "file:///android_asset/attachments/" + imgPath.substringAfterLast("attachments/")
            } else {
                imgPath
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(assetPath)
                    .crossfade(true)
                    .build(),
                contentDescription = altText,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            lastEnd = match.range.last + 1
        }

        val textAfter = text.substring(lastEnd)
        if (textAfter.isNotBlank()) {
            Text(text = textAfter, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
