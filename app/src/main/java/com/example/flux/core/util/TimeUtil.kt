package com.example.flux.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object TimeUtil {
    fun getCurrentIsoTime(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = tz
        return df.format(Date())
    }

    fun getCurrentDate(): String {
        val tz = TimeZone.getDefault()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        df.timeZone = tz
        return df.format(Date())
    }

    fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }
}
