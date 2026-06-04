package com.example.flux.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

class TodoWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { preferences ->
            preferences.remove(TodoWidgetOptions.recentlyCompletedTodoIdsKey)
        }
        delay(180)
        TodoWidget().updateAll(context)
    }
}

class TodoWidgetToggleFilterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { preferences ->
            val current = TodoWidgetFilter.fromPreference(preferences[TodoWidgetOptions.filterKey])
            preferences[TodoWidgetOptions.filterKey] = current.toggled().preferenceValue
            preferences.remove(TodoWidgetOptions.recentlyCompletedTodoIdsKey)
        }
        TodoWidget().updateAll(context)
    }
}

class TodoWidgetCompleteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters[TodoWidgetOptions.todoIdParameterKey] ?: return
        val targetStatus = parameters[TodoWidgetOptions.targetStatusParameterKey]
            ?.takeIf { it == "completed" || it == "pending" }
            ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxWidgetEntryPoint::class.java
        )
        val repository = entryPoint.todoRepository()
        val todo = repository.getTodoById(todoId) ?: return

        if (todo.status != targetStatus) {
            val now = TimeUtil.getCurrentIsoTime()
            repository.saveTodoWithHistory(
                todo.copy(
                    status = targetStatus,
                    completedAt = if (targetStatus == "completed") now else null,
                    updatedAt = now,
                    version = todo.version + 1
                ),
                action = if (targetStatus == "completed") "complete" else "reopen",
                summary = if (targetStatus == "completed") "\u6807\u8bb0\u5b8c\u6210" else "\u91cd\u65b0\u6253\u5f00"
            )
        }

        updateAppWidgetState(context, glanceId) { preferences ->
            val current = preferences[TodoWidgetOptions.recentlyCompletedTodoIdsKey].orEmpty()
            preferences[TodoWidgetOptions.recentlyCompletedTodoIdsKey] = if (targetStatus == "completed") {
                current + todoId
            } else {
                current - todoId
            }
        }
        TodoWidget().updateAll(context)
    }
}

class MonthWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        MonthWidget().updateClicked(context, glanceId)
    }
}

class MonthWidgetToggleSignalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val signal = MonthWidgetSignal.fromParameterValue(
            parameters[MonthWidgetOptions.signalParameterKey]
        ) ?: return
        val preferenceKey = MonthWidgetOptions.keyFor(signal)

        updateAppWidgetState(context, glanceId) { preferences ->
            val current = preferences[preferenceKey] ?: true
            preferences[preferenceKey] = !current
        }
        MonthWidget().updateClicked(context, glanceId)
    }
}

private suspend fun GlanceAppWidget.updateClicked(context: Context, glanceId: GlanceId) {
    update(context, glanceId)
}
