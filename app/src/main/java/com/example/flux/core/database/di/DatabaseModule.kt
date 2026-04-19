package com.example.flux.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.database.dao.DiaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFluxDatabase(@ApplicationContext context: Context): FluxDatabase {
        return Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            "flux.db"
        )
            .createFromAsset("flux.db")
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
    fun provideEventDao(database: FluxDatabase): com.example.flux.core.database.dao.EventDao {
        return database.eventDao()
    }

    @Provides
    fun provideHolidayDao(database: FluxDatabase): com.example.flux.core.database.dao.HolidayDao {
        return database.holidayDao()
    }
}
