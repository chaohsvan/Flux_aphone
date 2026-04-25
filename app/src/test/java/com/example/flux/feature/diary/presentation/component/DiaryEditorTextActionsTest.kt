package com.example.flux.feature.diary.presentation.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryEditorTextActionsTest {

    @Test
    fun `wrap selection surrounds selected text`() {
        val value = TextFieldValue(
            text = "hello world",
            selection = TextRange(6, 11)
        )

        val updated = wrapSelection(value, "**", "**", "text")

        assertEquals("hello **world**", updated.text)
        assertEquals(TextRange(8, 13), updated.selection)
    }

    @Test
    fun `apply line prefix updates every selected line`() {
        val value = TextFieldValue(
            text = "first\nsecond",
            selection = TextRange(0, 12)
        )

        val updated = applyLinePrefix(value, "- ")

        assertEquals("- first\n- second", updated.text)
        assertEquals(TextRange(0, updated.text.length), updated.selection)
    }

    @Test
    fun `apply code style uses fenced block for multiline selection`() {
        val value = TextFieldValue(
            text = "alpha\nbeta",
            selection = TextRange(0, 10)
        )

        val updated = applyCodeStyle(value)

        assertEquals("```\nalpha\nbeta\n```", updated.text)
    }

    @Test
    fun `count occurrences ignores case`() {
        assertEquals(3, countOccurrences("Flux flux FLUX", "flux"))
    }
}
