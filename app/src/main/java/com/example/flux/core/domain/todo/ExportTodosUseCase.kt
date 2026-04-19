package com.example.flux.core.domain.todo

import android.content.Context
import android.net.Uri
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity
import com.example.flux.core.database.repository.TodoRepository
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class TodoExportFormat(
    val extension: String,
    val mimeType: String
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    MARKDOWN("md", "text/markdown")
}

class ExportTodosUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(
        context: Context,
        ids: Set<String>,
        destUri: Uri,
        format: TodoExportFormat
    ) {
        if (ids.isEmpty()) return

        val records = ids.mapNotNull { id ->
            val todo = todoRepository.getTodoById(id) ?: return@mapNotNull null
            TodoExportRecord(
                todo = todo,
                subtasks = todoRepository.getSubtasksSnapshotForTodo(id)
            )
        }

        val content = when (format) {
            TodoExportFormat.JSON -> records.toJson()
            TodoExportFormat.CSV -> records.toCsv()
            TodoExportFormat.MARKDOWN -> records.toMarkdown()
        }

        context.contentResolver.openOutputStream(destUri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }
    }
}

private data class TodoExportRecord(
    val todo: TodoEntity,
    val subtasks: List<TodoSubtaskEntity>
)

private fun List<TodoExportRecord>.toJson(): String {
    val root = JSONArray()
    forEach { record ->
        val todo = record.todo
        val subtasks = JSONArray()
        record.subtasks.forEach { subtask ->
            subtasks.put(
                JSONObject()
                    .put("id", subtask.id)
                    .put("title", subtask.title)
                    .put("is_completed", subtask.isCompleted == 1)
                    .put("sort_order", subtask.sortOrder)
                    .put("created_at", subtask.createdAt)
                    .put("updated_at", subtask.updatedAt)
            )
        }

        root.put(
            JSONObject()
                .put("id", todo.id)
                .put("title", todo.title)
                .put("description", todo.description)
                .put("status", todo.status)
                .put("priority", todo.priority)
                .put("project_id", todo.projectId)
                .put("start_at", todo.startAt)
                .put("due_at", todo.dueAt)
                .put("completed_at", todo.completedAt)
                .put("reminder_minutes", todo.reminderMinutes)
                .put("sort_order", todo.sortOrder)
                .put("is_important", todo.isImportant == 1)
                .put("created_at", todo.createdAt)
                .put("updated_at", todo.updatedAt)
                .put("subtasks", subtasks)
        )
    }
    return root.toString(2)
}

private fun List<TodoExportRecord>.toCsv(): String {
    val rows = mutableListOf<List<String>>()
    rows += listOf(
        "title",
        "description",
        "status",
        "priority",
        "project_id",
        "start_at",
        "due_at",
        "completed_at",
        "reminder_minutes",
        "subtasks"
    )

    forEach { record ->
        val todo = record.todo
        rows += listOf(
            todo.title,
            todo.description,
            todo.status,
            todo.priority,
            todo.projectId.orEmpty(),
            todo.startAt.orEmpty(),
            todo.dueAt.orEmpty(),
            todo.completedAt.orEmpty(),
            todo.reminderMinutes?.toString().orEmpty(),
            record.subtasks.joinToString(" | ") { subtask ->
                "${if (subtask.isCompleted == 1) "[x]" else "[ ]"} ${subtask.title}"
            }
        )
    }

    return rows.joinToString("\n") { row -> row.joinToString(",") { it.csvEscape() } }
}

private fun List<TodoExportRecord>.toMarkdown(): String {
    val records = this
    return buildString {
        appendLine("# Flux Todo Export")
        appendLine()
        records.forEach { record ->
            val todo = record.todo
            appendLine("- ${if (todo.status == "completed") "[x]" else "[ ]"} ${todo.title}")
            appendLine("  - status: ${todo.status}")
            appendLine("  - priority: ${todo.priority}")
            todo.dueAt?.let { appendLine("  - due: $it") }
            todo.startAt?.let { appendLine("  - start: $it") }
            todo.reminderMinutes?.let { appendLine("  - reminder: $it minutes before") }
            if (todo.description.isNotBlank()) {
                appendLine("  - note: ${todo.description.replace("\n", " ")}")
            }
            record.subtasks.forEach { subtask ->
                appendLine("  - ${if (subtask.isCompleted == 1) "[x]" else "[ ]"} ${subtask.title}")
            }
            appendLine()
        }
    }
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}
