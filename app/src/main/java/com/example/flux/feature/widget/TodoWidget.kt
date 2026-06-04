package com.example.flux.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.example.flux.R
import com.example.flux.app.navigation.AppDestinations
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
        val todosFlow = entryPoint.todoRepository().getActiveTodos()
        val initialTodos = todosFlow.first()
        val openTodoAction = openDestinationAction(context, AppDestinations.TODO)

        provideContent {
            val todos = todosFlow.collectAsState(initialTodos).value
            val preferences = currentState<Preferences>()
            val recentlyCompletedIds = preferences.recentlyCompletedTodoIds()
            val filter = preferences.todoWidgetFilter()
            val state = todos.toTodoWidgetState(recentlyCompletedIds, filter)
            TodoWidgetContent(state, openTodoAction)
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

@Composable
private fun TodoWidgetContent(state: TodoWidgetState, openTodoAction: Action) {
    Column(modifier = GlanceModifier.widgetContainer()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Labels.TODO,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(openTodoAction),
                style = TextStyle(
                    color = FluxWidgetTheme.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            TodoWidgetFilterChip(state)
            Text(
                text = state.today.takeLast(5),
                modifier = GlanceModifier.padding(end = 12.dp),
                style = TextStyle(
                    color = FluxWidgetTheme.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = Labels.REFRESH,
                modifier = GlanceModifier
                    .clickable(actionRunCallback<TodoWidgetRefreshAction>())
                    .size(36.dp)
                    .padding(5.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (state.visibleTodos.isEmpty()) {
            Text(
                text = if (state.filter == TodoWidgetFilter.COMPLETED) {
                    Labels.NO_COMPLETED_TODO
                } else {
                    Labels.NO_TODO
                },
                modifier = GlanceModifier.clickable(openTodoAction),
                style = TextStyle(color = FluxWidgetTheme.muted, fontSize = 15.sp)
            )
        } else {
            state.visibleTodos.forEach { todo ->
                TodoWidgetRow(todo)
            }
        }
    }
}

@Composable
private fun TodoWidgetFilterChip(state: TodoWidgetState) {
    val isCompletedFilter = state.filter == TodoWidgetFilter.COMPLETED
    val label = if (isCompletedFilter) Labels.COMPLETED_TODO else Labels.PENDING_TODO
    val count = if (isCompletedFilter) state.completedCount else state.pendingCount
    Row(
        modifier = GlanceModifier
            .height(30.dp)
            .padding(end = 8.dp)
            .background(if (isCompletedFilter) FluxWidgetTheme.subtle else FluxWidgetTheme.surface)
            .clickable(actionRunCallback<TodoWidgetToggleFilterAction>())
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label $count",
            style = TextStyle(
                color = if (isCompletedFilter) FluxWidgetTheme.muted else FluxWidgetTheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    val dueDate = TimeUtil.localDatePart(todo.dueAt)
    val isCompleted = todo.status == "completed"
    val isHighPriority = todo.priority == "high" || todo.isImportant == 1
    val completeAction = actionRunCallback<TodoWidgetCompleteAction>(
        actionParametersOf(
            TodoWidgetOptions.todoIdParameterKey to todo.id,
            TodoWidgetOptions.targetStatusParameterKey to if (isCompleted) "pending" else "completed"
        )
    )
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(completeAction)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCompleted) Labels.CHECKED else Labels.UNCHECKED,
            modifier = GlanceModifier.width(20.dp),
            style = TextStyle(
                color = when {
                    isCompleted -> FluxWidgetTheme.primary
                    isHighPriority -> FluxWidgetTheme.danger
                    else -> FluxWidgetTheme.muted
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = todo.title.take(26),
            modifier = GlanceModifier.defaultWeight().padding(start = 6.dp),
            style = TextStyle(
                color = if (isCompleted) FluxWidgetTheme.muted else FluxWidgetTheme.text,
                fontSize = 15.sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            ),
            maxLines = 1
        )
        if (dueDate != null) {
            Text(
                text = dueDate.takeLast(5),
                style = TextStyle(
                    color = FluxWidgetTheme.muted,
                    fontSize = 12.sp,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                ),
                maxLines = 1
            )
        }
    }
}

private fun Preferences.recentlyCompletedTodoIds(): Set<String> {
    return this[TodoWidgetOptions.recentlyCompletedTodoIdsKey].orEmpty()
}

private fun Preferences.todoWidgetFilter(): TodoWidgetFilter {
    return TodoWidgetFilter.fromPreference(this[TodoWidgetOptions.filterKey])
}
