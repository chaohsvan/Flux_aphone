package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM calendar_holiday_overrides ORDER BY date ASC")
    fun getHolidayOverrides(): Flow<List<CalendarHolidayOverrideEntity>>

    @Query("SELECT * FROM calendar_holiday_overrides WHERE date = :date LIMIT 1")
    suspend fun getHolidayOverrideByDate(date: String): CalendarHolidayOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidayOverride(override: CalendarHolidayOverrideEntity)

    @Query("DELETE FROM calendar_holiday_overrides WHERE date = :date")
    suspend fun deleteHolidayOverride(date: String)
}
