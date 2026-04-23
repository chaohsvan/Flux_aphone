package com.example.flux.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.database.FluxPrepackagedDatabaseNormalizer
import com.example.flux.core.database.dao.DiaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

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

    @Provides
    @Singleton
    fun provideFluxDatabase(@ApplicationContext context: Context): FluxDatabase {
        return Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            "flux.db"
        )
            .createFromAsset(
                "flux.db",
                object : RoomDatabase.PrepackagedDatabaseCallback() {
                    override fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {
                        FluxPrepackagedDatabaseNormalizer.normalize(db)
                    }
                }
            )
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
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
}
