package com.example.flux.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.util.TimeUtil
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxWidgetEntryPoint::class.java
        )
        val state = entryPoint.todoRepository().getActiveTodos().first().toTodoWidgetState()

        provideContent {
            TodoWidgetContent(state)
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

@Composable
private fun TodoWidgetContent(state: TodoWidgetState) {
    Column(modifier = GlanceModifier.widgetContainer()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Labels.TODO,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(
                    color = FluxWidgetTheme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = state.today.takeLast(5),
                modifier = GlanceModifier.padding(end = 12.dp),
                style = TextStyle(
                    color = FluxWidgetTheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = Labels.REFRESH,
                modifier = GlanceModifier
                    .width(24.dp)
                    .height(24.dp)
                    .clickable(actionRunCallback<TodoWidgetRefreshAction>())
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (state.visibleTodos.isEmpty()) {
            Text(
                text = Labels.NO_TODO,
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(color = FluxWidgetTheme.muted, fontSize = 13.sp)
            )
        } else {
            state.visibleTodos.forEach { todo ->
                TodoWidgetRow(todo)
            }
        }
    }
}

@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    val dueDate = TimeUtil.localDatePart(todo.dueAt)
    val marker = if (todo.priority == "high" || todo.isImportant == 1) "!" else "-"
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = marker,
            style = TextStyle(
                color = if (marker == "!") FluxWidgetTheme.danger else FluxWidgetTheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = todo.title.take(26),
            modifier = GlanceModifier.defaultWeight().padding(start = 6.dp),
            style = TextStyle(color = FluxWidgetTheme.text, fontSize = 13.sp)
        )
        if (dueDate != null) {
            Text(
                text = dueDate.takeLast(5),
                style = TextStyle(color = FluxWidgetTheme.muted, fontSize = 11.sp)
            )
        }
    }
}
