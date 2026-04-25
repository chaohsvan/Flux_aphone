package com.example.flux.feature.diary.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParsingTest {

    @Test
    fun `parse markdown blocks keeps structure for common syntax`() {
        val markdown = """
            # 标题
            普通段落
            
            > 引用
            - 列表
            1. 有序
            - [x] 已完成
            ---
            ![封面](attachments/cover.png)
            [audio:录音](attachments/demo.m4a)
            ```kotlin
            println("hi")
            ```
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)

        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals("标题", (blocks[0] as MarkdownBlock.Heading).text)
        assertTrue(blocks.any { it is MarkdownBlock.Paragraph && it.text == "普通段落" })
        assertTrue(blocks.any { it is MarkdownBlock.Quote && it.text == "引用" })
        assertTrue(blocks.any { it is MarkdownBlock.ListEntry && it.text == "列表" })
        assertTrue(blocks.any { it is MarkdownBlock.ListEntry && it.orderedPrefix == "1." })
        assertTrue(blocks.any { it is MarkdownBlock.ListEntry && it.taskState == true })
        assertTrue(blocks.any { it is MarkdownBlock.Divider })
        assertTrue(blocks.any { it is MarkdownBlock.Image && it.path == "attachments/cover.png" })
        assertTrue(blocks.any { it is MarkdownBlock.Attachment && it.kind == "audio" && it.label == "录音" })
        assertTrue(blocks.any { it is MarkdownBlock.CodeBlock && it.text.contains("println") })
    }

    @Test
    fun `attachment without explicit label falls back to file name`() {
        val blocks = parseMarkdownBlocks("[file:](attachments/archive/report.pdf)")

        val attachment = blocks.single() as MarkdownBlock.Attachment
        assertEquals("file", attachment.kind)
        assertEquals("report.pdf", attachment.label)
        assertEquals("attachments/archive/report.pdf", attachment.path)
    }
}
