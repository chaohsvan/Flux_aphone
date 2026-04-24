package com.example.flux.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.database.FluxPrepackagedDatabaseNormalizer
import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.util.DataPaths
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_8 = object : Migration(1, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createLegacySourceTables(db)
            ensureColumn(db, "todos", "is_my_day", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "todos", "recurrence", "TEXT NOT NULL DEFAULT 'none'")
            ensureColumn(db, "todos", "recurrence_interval", "INTEGER NOT NULL DEFAULT 1")
            ensureColumn(db, "todos", "recurrence_until", "TEXT")
            ensureColumn(db, "todos", "parent_todo_id", "TEXT")
            FluxPrepackagedDatabaseNormalizer.normalize(db)
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_subtasks_todo ON todo_subtasks(todo_id, deleted_at)")
            db.execSQL("DROP INDEX IF EXISTS idx_todo_history_todo")
            db.execSQL(
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

            val hasOldHistoryTable = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='todo_history'"
            ).use { cursor ->
                cursor.moveToFirst()
            }

            if (hasOldHistoryTable) {
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO todo_history_room (
                        id, todo_id, action, summary, payload_json, created_at
                    )
                    SELECT id, todo_id, action, summary, payload_json, created_at
                    FROM todo_history
                    WHERE id IS NOT NULL
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE todo_history")
            }

            db.execSQL("ALTER TABLE todo_history_room RENAME TO todo_history")
            db.execSQL("CREATE INDEX idx_todo_history_todo ON todo_history(todo_id, created_at)")
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            FluxPrepackagedDatabaseNormalizer.rebuildDiarySearchIndex(db)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_subtasks_todo ON todo_subtasks(todo_id, deleted_at)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            FluxPrepackagedDatabaseNormalizer.rebuildDiarySearchIndex(db)
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            FluxPrepackagedDatabaseNormalizer.rebuildDiarySearchIndex(db)
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS calendar_static_holidays (
                    day TEXT NOT NULL PRIMARY KEY,
                    is_holiday INTEGER NOT NULL DEFAULT 1,
                    name TEXT,
                    source TEXT NOT NULL DEFAULT 'chinese-days',
                    updated_at TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS attachment_metadata (
                    relative_path TEXT NOT NULL PRIMARY KEY,
                    file_name TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    size_bytes INTEGER NOT NULL,
                    modified_at INTEGER NOT NULL,
                    sha256 TEXT NOT NULL,
                    reference_count INTEGER NOT NULL DEFAULT 0,
                    last_scanned_at TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_attachment_metadata_kind ON attachment_metadata(kind)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_attachment_metadata_size ON attachment_metadata(size_bytes)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_attachment_metadata_modified ON attachment_metadata(modified_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_attachment_metadata_sha ON attachment_metadata(sha256)")
        }
    }

    @Provides
    @Singleton
    fun provideFluxDatabase(@ApplicationContext context: Context): FluxDatabase {
        val databaseFile = DataPaths.databaseFile(context)
        return Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            databaseFile.absolutePath
        )
            .createFromAsset(
                "flux.db",
                object : RoomDatabase.PrepackagedDatabaseCallback() {
                    override fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {
                        FluxPrepackagedDatabaseNormalizer.normalize(db)
                    }
                }
            )
            .addMigrations(MIGRATION_1_8, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .build()
    }

    @Provides
    fun provideDiaryDao(database: FluxDatabase): DiaryDao {
        return database.diaryDao()
    }

    @Provides
    fun provideTodoDao(database: FluxDatabase): com.example.flux.core.database.dao.TodoDao {
        return database.todoDao()
    }

    @Provides
    fun provideTodoSubtaskDao(database: FluxDatabase): com.example.flux.core.database.dao.TodoSubtaskDao {
        return database.todoSubtaskDao()
    }

    @Provides
    fun provideTodoProjectDao(database: FluxDatabase): com.example.flux.core.database.dao.TodoProjectDao {
        return database.todoProjectDao()
    }

    @Provides
    fun provideTodoHistoryDao(database: FluxDatabase): com.example.flux.core.database.dao.TodoHistoryDao {
        return database.todoHistoryDao()
    }

    @Provides
    fun provideEventDao(database: FluxDatabase): com.example.flux.core.database.dao.EventDao {
        return database.eventDao()
    }

    @Provides
    fun provideHolidayDao(database: FluxDatabase): com.example.flux.core.database.dao.HolidayDao {
        return database.holidayDao()
    }

    @Provides
    fun provideAttachmentMetadataDao(database: FluxDatabase): com.example.flux.core.database.dao.AttachmentMetadataDao {
        return database.attachmentMetadataDao()
    }

    private fun createLegacySourceTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS todo_projects (
                id TEXT PRIMARY KEY,
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS todo_history (
                id TEXT PRIMARY KEY,
                todo_id TEXT NOT NULL,
                action TEXT NOT NULL,
                summary TEXT NOT NULL,
                payload_json TEXT NOT NULL DEFAULT '{}',
                created_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS calendar_holidays (
                day TEXT PRIMARY KEY,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                is_holiday INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS calendar_static_holidays (
                day TEXT PRIMARY KEY,
                is_holiday INTEGER NOT NULL DEFAULT 1,
                name TEXT,
                source TEXT NOT NULL DEFAULT 'chinese-days',
                updated_at TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun ensureColumn(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
        val hasColumn = db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true
                    break
                }
            }
            found
        }
        if (!hasColumn) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }
}
