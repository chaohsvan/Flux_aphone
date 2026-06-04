package com.example.flux.feature.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.action.ActionParameters

enum class MonthWidgetSignal(
    val parameterValue: String,
    val label: String
) {
    DIARY("diary", "日记"),
    TODO("todo", "待办"),
    EVENT("event", "事件"),
    TRASH("trash", "回收");

    companion object {
        fun fromParameterValue(value: String?): MonthWidgetSignal? {
            return entries.firstOrNull { it.parameterValue == value }
        }
    }
}

data class MonthWidgetVisibility(
    val showDiary: Boolean = true,
    val showTodo: Boolean = true,
    val showEvent: Boolean = true,
    val showTrash: Boolean = true
) {
    fun isVisible(signal: MonthWidgetSignal): Boolean {
        return when (signal) {
            MonthWidgetSignal.DIARY -> showDiary
            MonthWidgetSignal.TODO -> showTodo
            MonthWidgetSignal.EVENT -> showEvent
            MonthWidgetSignal.TRASH -> showTrash
        }
    }
}

object MonthWidgetOptions {
    val signalParameterKey = ActionParameters.Key<String>("month_widget_signal")

    val showDiaryKey = booleanPreferencesKey("month_widget_show_diary")
    val showTodoKey = booleanPreferencesKey("month_widget_show_todo")
    val showEventKey = booleanPreferencesKey("month_widget_show_event")
    val showTrashKey = booleanPreferencesKey("month_widget_show_trash")

    fun keyFor(signal: MonthWidgetSignal): Preferences.Key<Boolean> {
        return when (signal) {
            MonthWidgetSignal.DIARY -> showDiaryKey
            MonthWidgetSignal.TODO -> showTodoKey
            MonthWidgetSignal.EVENT -> showEventKey
            MonthWidgetSignal.TRASH -> showTrashKey
        }
    }
}

fun Preferences.monthWidgetVisibility(): MonthWidgetVisibility {
    return MonthWidgetVisibility(
        showDiary = this[MonthWidgetOptions.showDiaryKey] ?: true,
        showTodo = this[MonthWidgetOptions.showTodoKey] ?: true,
        showEvent = this[MonthWidgetOptions.showEventKey] ?: true,
        showTrash = this[MonthWidgetOptions.showTrashKey] ?: true
    )
}
