package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.database.entity.CalendarStaticHolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM calendar_holidays ORDER BY day ASC")
    fun getHolidayOverrides(): Flow<List<CalendarHolidayOverrideEntity>>

    @Query("SELECT * FROM calendar_static_holidays ORDER BY day ASC")
    fun getStaticHolidays(): Flow<List<CalendarStaticHolidayEntity>>

    @Query("SELECT * FROM calendar_holidays WHERE day = :date LIMIT 1")
    suspend fun getHolidayOverrideByDate(date: String): CalendarHolidayOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidayOverride(override: CalendarHolidayOverrideEntity)

    @Query("DELETE FROM calendar_holidays WHERE day = :date")
    suspend fun deleteHolidayOverride(date: String)
}
