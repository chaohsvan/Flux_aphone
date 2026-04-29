package com.example.flux.core.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class WeatherAppBinding(
    val packageName: String,
    val activityName: String?,
    val displayName: String
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun observeWeekStartDay(): Flow<Int> = callbackFlow {
        trySend(getWeekStartDay())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_WEEK_START_DAY) {
                trySend(getWeekStartDay())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun getWeekStartDay(): Int {
        return preferences.getInt(KEY_WEEK_START_DAY, Calendar.SUNDAY)
            .takeIf { it == Calendar.SUNDAY || it == Calendar.MONDAY }
            ?: Calendar.SUNDAY
    }

    fun setWeekStartDay(value: Int) {
        val normalized = if (value == Calendar.MONDAY) Calendar.MONDAY else Calendar.SUNDAY
        preferences.edit().putInt(KEY_WEEK_START_DAY, normalized).apply()
    }

    fun observeReminderSoundEnabled(): Flow<Boolean> = callbackFlow {
        trySend(isReminderSoundEnabled())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_REMINDER_SOUND_ENABLED) {
                trySend(isReminderSoundEnabled())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun isReminderSoundEnabled(): Boolean {
        return preferences.getBoolean(KEY_REMINDER_SOUND_ENABLED, true)
    }

    fun setReminderSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_REMINDER_SOUND_ENABLED, enabled).apply()
    }

    fun observeWeatherAppBinding(): Flow<WeatherAppBinding?> = callbackFlow {
        trySend(getWeatherAppBinding())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in WEATHER_APP_KEYS) {
                trySend(getWeatherAppBinding())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun getWeatherAppBinding(): WeatherAppBinding? {
        val packageName = preferences.getString(KEY_WEATHER_APP_PACKAGE, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val displayName = preferences.getString(KEY_WEATHER_APP_DISPLAY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: packageName
        val activityName = preferences.getString(KEY_WEATHER_APP_ACTIVITY, null)
            ?.takeIf { it.isNotBlank() }
        return WeatherAppBinding(
            packageName = packageName,
            activityName = activityName,
            displayName = displayName
        )
    }

    fun setWeatherAppBinding(binding: WeatherAppBinding) {
        preferences.edit()
            .putString(KEY_WEATHER_APP_PACKAGE, binding.packageName)
            .putString(KEY_WEATHER_APP_ACTIVITY, binding.activityName)
            .putString(KEY_WEATHER_APP_DISPLAY_NAME, binding.displayName)
            .apply()
    }

    fun clearWeatherAppBinding() {
        preferences.edit()
            .remove(KEY_WEATHER_APP_PACKAGE)
            .remove(KEY_WEATHER_APP_ACTIVITY)
            .remove(KEY_WEATHER_APP_DISPLAY_NAME)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "flux_app_preferences"
        const val KEY_WEEK_START_DAY = "week_start_day"
        const val KEY_REMINDER_SOUND_ENABLED = "reminder_sound_enabled"
        const val KEY_WEATHER_APP_PACKAGE = "weather_app_package"
        const val KEY_WEATHER_APP_ACTIVITY = "weather_app_activity"
        const val KEY_WEATHER_APP_DISPLAY_NAME = "weather_app_display_name"
        val WEATHER_APP_KEYS = setOf(
            KEY_WEATHER_APP_PACKAGE,
            KEY_WEATHER_APP_ACTIVITY,
            KEY_WEATHER_APP_DISPLAY_NAME
        )
    }
}
