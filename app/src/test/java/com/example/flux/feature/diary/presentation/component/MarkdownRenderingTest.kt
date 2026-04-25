package com.example.flux.feature.diary.presentation.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRenderingTest {

    @Test
    fun `build annotated string keeps markdown links as annotations`() {
        val annotated = buildMarkdownAnnotatedString(
            text = "查看 [官网](https://example.com) 了解详情",
            highlightQuery = "",
            linkColor = Color.Blue,
            inlineCodeBackground = Color.Gray,
            highlightColor = Color.Yellow
        )

        val annotation = annotated.getStringAnnotations(LINK_TAG, 0, annotated.length).single()
        assertEquals("https://example.com", annotation.item)
        assertTrue(annotated.text.contains("官网"))
    }

    @Test
    fun `build annotated string highlights all query matches`() {
        val annotated = buildMarkdownAnnotatedString(
            text = "alpha beta alpha",
            highlightQuery = "alpha",
            linkColor = Color.Blue,
            inlineCodeBackground = Color.Gray,
            highlightColor = Color.Yellow
        )

        assertEquals("alpha beta alpha", annotated.text)
        assertEquals(2, annotated.spanStyles.count { it.item.background == Color.Yellow })
    }
}
