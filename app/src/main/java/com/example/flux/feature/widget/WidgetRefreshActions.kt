package com.example.flux.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback

class TodoWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TodoWidget().updateClicked(context, glanceId)
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

private suspend fun GlanceAppWidget.updateClicked(context: Context, glanceId: GlanceId) {
    update(context, glanceId)
}
