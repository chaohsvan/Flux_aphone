package com.example.flux.feature.widget

import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.action.ActionParameters

object TodoWidgetOptions {
    val todoIdParameterKey = ActionParameters.Key<String>("todo_widget_todo_id")
    val targetStatusParameterKey = ActionParameters.Key<String>("todo_widget_target_status")
    val recentlyCompletedTodoIdsKey = stringSetPreferencesKey("todo_widget_recently_completed_ids")
    val filterKey = stringPreferencesKey("todo_widget_filter")
}

enum class TodoWidgetFilter(val preferenceValue: String) {
    PENDING("pending"),
    COMPLETED("completed");

    fun toggled(): TodoWidgetFilter {
        return if (this == PENDING) COMPLETED else PENDING
    }

    companion object {
        fun fromPreference(value: String?): TodoWidgetFilter {
            return entries.firstOrNull { it.preferenceValue == value } ?: PENDING
        }
    }
}
