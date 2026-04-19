package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.HolidayDao
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HolidayRepository @Inject constructor(
    private val holidayDao: HolidayDao
) {
    fun getHolidayOverrides(): Flow<List<CalendarHolidayOverrideEntity>> {
        return holidayDao.getHolidayOverrides()
    }

    suspend fun toggleHolidayOverride(date: String, defaultIsHoliday: Boolean) {
        val current = holidayDao.getHolidayOverrideByDate(date)
        val effectiveIsHoliday = current?.isHoliday?.let { it == 1 } ?: defaultIsHoliday
        val nextIsHoliday = !effectiveIsHoliday

        if (nextIsHoliday == defaultIsHoliday) {
            holidayDao.deleteHolidayOverride(date)
        } else {
            val now = TimeUtil.getCurrentIsoTime()
            holidayDao.insertHolidayOverride(
                CalendarHolidayOverrideEntity(
                    date = date,
                    isHoliday = if (nextIsHoliday) 1 else 0,
                    label = if (nextIsHoliday) "手动假期" else "手动工作日",
                    createdAt = current?.createdAt ?: now,
                    updatedAt = now,
                    version = (current?.version ?: 0) + 1
                )
            )
        }
    }
}
