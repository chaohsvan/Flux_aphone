package com.example.flux.feature.diary.presentation.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max
import kotlin.math.min

fun wrapSelection(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val selected = value.text.substring(start, end)
    val body = selected.ifBlank { placeholder }
    val replacement = prefix + body + suffix
    val updated = value.text.replaceRange(start, end, replacement)
    val bodyStart = start + prefix.length
    return value.copy(
        text = updated,
        selection = TextRange(bodyStart, bodyStart + body.length)
    )
}

fun applyLinePrefix(
    value: TextFieldValue,
    prefix: String
): TextFieldValue {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val lineStart = value.text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
    val lineEnd = value.text.indexOf('\n', end).let { if (it < 0) value.text.length else it }
    val target = value.text.substring(lineStart, lineEnd)
    val replaced = target.lines().joinToString("\n") { line ->
        if (line.isBlank()) prefix.trimEnd() else prefix + line
    }
    val updated = value.text.replaceRange(lineStart, lineEnd, replaced)
    return value.copy(
        text = updated,
        selection = TextRange(lineStart, lineStart + replaced.length)
    )
}

fun applyCodeStyle(value: TextFieldValue): TextFieldValue {
    val selected = value.text.substring(
        min(value.selection.start, value.selection.end),
        max(value.selection.start, value.selection.end)
    )
    return if (selected.contains('\n') || selected.isBlank()) {
        wrapSelection(value, "```\n", "\n```", "code")
    } else {
        wrapSelection(value, "`", "`", "code")
    }
}

fun insertBlock(
    value: TextFieldValue,
    block: String
): TextFieldValue {
    val cursor = value.selection.end
    val updated = value.text.replaceRange(cursor, cursor, block)
    val newCursor = cursor + block.length
    return value.copy(text = updated, selection = TextRange(newCursor))
}

fun countOccurrences(text: String, query: String): Int {
    val needle = query.trim()
    if (needle.isBlank()) return 0
    var count = 0
    var start = 0
    val source = text.lowercase()
    val target = needle.lowercase()
    while (true) {
        val index = source.indexOf(target, start)
        if (index < 0) return count
        count += 1
        start = index + target.length
    }
}
