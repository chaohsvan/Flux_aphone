package com.example.flux.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object FluxPrepackagedDatabaseNormalizer {
    fun normalize(db: SupportSQLiteDatabase) {
        normalizeWith { sql -> db.execSQL(sql) }
    }

    fun normalize(db: SQLiteDatabase) {
        normalizeWith { sql -> db.execSQL(sql) }
    }

    fun rebuildDiaryFts(db: SupportSQLiteDatabase) {
        rebuildDiaryFts { sql -> db.execSQL(sql) }
    }

    private fun normalizeWith(exec: (String) -> Unit) {
        exec("PRAGMA foreign_keys=OFF")
        exec("BEGIN TRANSACTION")
        try {
            rebuildDiaries(exec)
            rebuildDiaryTags(exec)
            rebuildDiaryFts(exec)
            rebuildTodoProjects(exec)
            rebuildTodos(exec)
            rebuildTodoSubtasks(exec)
            rebuildTodoHistory(exec)
            rebuildCalendarEvents(exec)
            rebuildCalendarHolidays(exec)
            exec("PRAGMA user_version=4")
            exec("COMMIT")
        } catch (throwable: Throwable) {
            exec("ROLLBACK")
            throw throwable
        } finally {
            exec("PRAGMA foreign_keys=ON")
        }
    }

    private fun rebuildDiaries(exec: (String) -> Unit) {
        exec("DROP INDEX IF EXISTS idx_diaries_one_active_per_day")
        exec("DROP INDEX IF EXISTS idx_diaries_mood")
        exec("DROP INDEX IF EXISTS idx_diaries_date")
        exec(
            """
            CREATE TABLE IF NOT EXISTS diaries_room (
                id TEXT NOT NULL PRIMARY KEY,
                entry_date TEXT NOT NULL,
                title TEXT NOT NULL,
                content_md TEXT NOT NULL DEFAULT '',
                mood TEXT,
                weather TEXT,
                location_name TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                word_count INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                entry_time TEXT,
                restored_at TEXT,
                restored_into_id TEXT
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO diaries_room (
                id, entry_date, title, content_md, mood, weather, location_name,
                is_favorite, word_count, created_at, updated_at, deleted_at, version,
                entry_time, restored_at, restored_into_id
            )
            SELECT
                id, entry_date, title, content_md, mood, weather, location_name,
                is_favorite, word_count, created_at, updated_at, deleted_at, version,
                entry_time, restored_at, restored_into_id
            FROM diaries
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE diaries")
        exec("ALTER TABLE diaries_room RENAME TO diaries")
        exec("CREATE INDEX idx_diaries_date ON diaries(entry_date, deleted_at)")
        exec("CREATE INDEX idx_diaries_mood ON diaries(mood, deleted_at)")
        exec(
            """
            CREATE UNIQUE INDEX idx_diaries_one_active_per_day
            ON diaries(entry_date)
            WHERE deleted_at IS NULL
            """.trimIndent()
        )
    }

    private fun rebuildDiaryTags(exec: (String) -> Unit) {
        exec(
            """
            CREATE TABLE IF NOT EXISTS diary_tags_room (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                color TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO diary_tags_room (
                id, name, color, created_at, updated_at, deleted_at, version
            )
            SELECT id, name, color, created_at, updated_at, deleted_at, version
            FROM diary_tags
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE diary_tags")
        exec("ALTER TABLE diary_tags_room RENAME TO diary_tags")
    }

    private fun rebuildDiaryFts(exec: (String) -> Unit) {
        exec("DROP TABLE IF EXISTS diaries_fts")
        exec(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS diaries_fts USING fts5(
                diary_id UNINDEXED,
                entry_date,
                entry_time,
                content_md,
                mood,
                weather,
                location_name,
                tags
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT INTO diaries_fts(
                diary_id, entry_date, entry_time, content_md, mood, weather, location_name, tags
            )
            SELECT
                diaries.id,
                diaries.entry_date,
                diaries.entry_time,
                diaries.content_md,
                diaries.mood,
                diaries.weather,
                diaries.location_name,
                COALESCE(GROUP_CONCAT(diary_tags.name, ' '), '')
            FROM diaries
            LEFT JOIN diary_tag_links
                ON diary_tag_links.diary_id = diaries.id
                AND diary_tag_links.deleted_at IS NULL
            LEFT JOIN diary_tags
                ON diary_tags.id = diary_tag_links.tag_id
                AND diary_tags.deleted_at IS NULL
            WHERE diaries.deleted_at IS NULL
            GROUP BY
                diaries.id,
                diaries.entry_date,
                diaries.entry_time,
                diaries.content_md,
                diaries.mood,
                diaries.weather,
                diaries.location_name
            """.trimIndent()
        )
    }

    private fun rebuildTodoProjects(exec: (String) -> Unit) {
        exec(
            """
            CREATE TABLE IF NOT EXISTS todo_projects_room (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT,
                sort_order INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO todo_projects_room (
                id, name, color, sort_order, created_at, updated_at, deleted_at, version
            )
            SELECT id, name, color, sort_order, created_at, updated_at, deleted_at, version
            FROM todo_projects
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE todo_projects")
        exec("ALTER TABLE todo_projects_room RENAME TO todo_projects")
    }

    private fun rebuildTodos(exec: (String) -> Unit) {
        exec("DROP INDEX IF EXISTS idx_todos_due")
        exec("DROP INDEX IF EXISTS idx_todos_project")
        exec("DROP INDEX IF EXISTS idx_todos_status")
        exec(
            """
            CREATE TABLE IF NOT EXISTS todos_room (
                id TEXT NOT NULL PRIMARY KEY,
                project_id TEXT,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'pending',
                priority TEXT NOT NULL DEFAULT 'none',
                due_at TEXT,
                start_at TEXT,
                completed_at TEXT,
                sort_order INTEGER NOT NULL DEFAULT 0,
                is_my_day INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                is_important INTEGER NOT NULL DEFAULT 0,
                reminder_minutes INTEGER,
                recurrence TEXT NOT NULL DEFAULT 'none',
                recurrence_interval INTEGER NOT NULL DEFAULT 1,
                recurrence_until TEXT,
                parent_todo_id TEXT,
                FOREIGN KEY(project_id) REFERENCES todo_projects(id)
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO todos_room (
                id, project_id, title, description, status, priority, due_at, start_at,
                completed_at, sort_order, is_my_day, created_at, updated_at, deleted_at,
                version, is_important, reminder_minutes, recurrence, recurrence_interval,
                recurrence_until, parent_todo_id
            )
            SELECT
                id, project_id, title, description, status, priority, due_at, start_at,
                completed_at, sort_order, is_my_day, created_at, updated_at, deleted_at,
                version, is_important, reminder_minutes, recurrence, recurrence_interval,
                recurrence_until, parent_todo_id
            FROM todos
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE todos")
        exec("ALTER TABLE todos_room RENAME TO todos")
        exec("CREATE INDEX idx_todos_due ON todos(due_at, status, deleted_at)")
        exec("CREATE INDEX idx_todos_project ON todos(project_id, deleted_at)")
        exec("CREATE INDEX idx_todos_status ON todos(status, deleted_at)")
    }

    private fun rebuildTodoSubtasks(exec: (String) -> Unit) {
        exec("DROP INDEX IF EXISTS idx_todo_subtasks_todo")
        exec(
            """
            CREATE TABLE IF NOT EXISTS todo_subtasks_room (
                id TEXT NOT NULL PRIMARY KEY,
                todo_id TEXT NOT NULL,
                title TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                sort_order INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(todo_id) REFERENCES todos(id)
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO todo_subtasks_room (
                id, todo_id, title, is_completed, sort_order, created_at, updated_at,
                deleted_at, version
            )
            SELECT
                id, todo_id, title, is_completed, sort_order, created_at, updated_at,
                deleted_at, version
            FROM todo_subtasks
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE todo_subtasks")
        exec("ALTER TABLE todo_subtasks_room RENAME TO todo_subtasks")
        exec("CREATE INDEX idx_todo_subtasks_todo ON todo_subtasks(todo_id, deleted_at)")
    }

    private fun rebuildTodoHistory(exec: (String) -> Unit) {
        exec("DROP INDEX IF EXISTS idx_todo_history_todo")
        exec(
            """
            CREATE TABLE IF NOT EXISTS todo_history_room (
                id TEXT NOT NULL PRIMARY KEY,
                todo_id TEXT NOT NULL,
                action TEXT NOT NULL,
                summary TEXT NOT NULL,
                payload_json TEXT NOT NULL DEFAULT '{}',
                created_at TEXT NOT NULL,
                FOREIGN KEY(todo_id) REFERENCES todos(id)
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO todo_history_room (
                id, todo_id, action, summary, payload_json, created_at
            )
            SELECT id, todo_id, action, summary, payload_json, created_at
            FROM todo_history
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE todo_history")
        exec("ALTER TABLE todo_history_room RENAME TO todo_history")
        exec("CREATE INDEX idx_todo_history_todo ON todo_history(todo_id, created_at)")
    }

    private fun rebuildCalendarEvents(exec: (String) -> Unit) {
        exec("DROP INDEX IF EXISTS idx_events_range")
        exec("DROP INDEX IF EXISTS idx_events_start")
        exec(
            """
            CREATE TABLE IF NOT EXISTS calendar_events_room (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                start_at TEXT NOT NULL,
                end_at TEXT NOT NULL,
                all_day INTEGER NOT NULL DEFAULT 0,
                color TEXT,
                location_name TEXT,
                reminder_minutes INTEGER,
                recurrence_rule TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                version INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO calendar_events_room (
                id, title, description, start_at, end_at, all_day, color, location_name,
                reminder_minutes, recurrence_rule, created_at, updated_at, deleted_at, version
            )
            SELECT
                id, title, description, start_at, end_at, all_day, color, location_name,
                reminder_minutes, recurrence_rule, created_at, updated_at, deleted_at, version
            FROM calendar_events
            WHERE id IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE calendar_events")
        exec("ALTER TABLE calendar_events_room RENAME TO calendar_events")
        exec("CREATE INDEX idx_events_range ON calendar_events(start_at, end_at, deleted_at)")
        exec("CREATE INDEX idx_events_start ON calendar_events(start_at, deleted_at)")
    }

    private fun rebuildCalendarHolidays(exec: (String) -> Unit) {
        exec(
            """
            CREATE TABLE IF NOT EXISTS calendar_holidays_room (
                day TEXT NOT NULL PRIMARY KEY,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                is_holiday INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT OR REPLACE INTO calendar_holidays_room (
                day, created_at, updated_at, is_holiday
            )
            SELECT day, created_at, updated_at, is_holiday
            FROM calendar_holidays
            WHERE day IS NOT NULL
            """.trimIndent()
        )
        exec("DROP TABLE calendar_holidays")
        exec("ALTER TABLE calendar_holidays_room RENAME TO calendar_holidays")
    }
}
