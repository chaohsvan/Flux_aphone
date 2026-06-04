package com.example.flux.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.flux.R
import com.example.flux.app.navigation.AppDestinations
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class MonthWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxWidgetEntryPoint::class.java
        )
        val state = entryPoint.calendarAggregatorUseCase().invoke().first().toMonthWidgetState()
        val openCalendarAction = openDestinationAction(context, AppDestinations.CALENDAR)

        provideContent {
            MonthWidgetContent(state, openCalendarAction)
        }
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

@Composable
private fun MonthWidgetContent(state: MonthWidgetState, openCalendarAction: Action) {
    val widgetSize = LocalSize.current
    val visibility = currentState<Preferences>().monthWidgetVisibility()
    val weekCount = (state.cells.size / DAYS_PER_WEEK).coerceAtLeast(MIN_MONTH_WIDGET_WEEK_COUNT)
    val rowHeight = monthRowHeight(widgetSize, weekCount)

    Column(
        modifier = GlanceModifier.widgetContainer()
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.title,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(openCalendarAction),
                style = TextStyle(
                    color = FluxWidgetTheme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = Labels.REFRESH,
                modifier = GlanceModifier
                    .clickable(actionRunCallback<MonthWidgetRefreshAction>())
                    .size(36.dp)
                    .padding(5.dp)
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        WeekHeader()
        Spacer(modifier = GlanceModifier.height(4.dp))
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            state.cells.chunked(7).forEach { week ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(rowHeight)
                ) {
                    week.forEach { cell ->
                        MonthDayCell(
                            cell,
                            modifier = GlanceModifier.defaultWeight(),
                            openCalendarAction = openCalendarAction,
                            visibility = visibility,
                            rowHeight = rowHeight
                        )
                    }
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        MonthLegend(visibility)
    }
}

private fun monthRowHeight(size: DpSize, weekCount: Int): Dp {
    val available = size.height - MONTH_WIDGET_FIXED_VERTICAL_SPACE
    return (available / weekCount).coerceAtLeast(MONTH_WIDGET_MIN_ROW_HEIGHT)
}

@Composable
private fun WeekHeader() {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Labels.WEEK_DAYS.forEach { label ->
            Text(
                text = label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = FluxWidgetTheme.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthDayCell(
    cell: MonthWidgetDayCell,
    modifier: GlanceModifier = GlanceModifier,
    openCalendarAction: Action,
    visibility: MonthWidgetVisibility,
    rowHeight: Dp
) {
    val contentColor = if (cell.isToday) FluxWidgetTheme.primary else FluxWidgetTheme.text
    val visibleSignals = cell.visibleSignals(visibility)
    val cellBackground = when {
        cell.isToday -> FluxWidgetTheme.surface
        visibleSignals.isNotEmpty() -> FluxWidgetTheme.subtle
        else -> FluxWidgetTheme.background
    }
    Column(
        modifier = modifier
            .height(rowHeight - 2.dp)
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .background(cellBackground)
            .clickable(openCalendarAction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = cell.day?.toString().orEmpty(),
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
        if (visibleSignals.isNotEmpty()) {
            Column(
                modifier = GlanceModifier.padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                visibleSignals.forEach { signal ->
                    SignalBar(signal.color)
                }
            }
        }
    }
}

private fun MonthWidgetDayCell.visibleSignals(
    visibility: MonthWidgetVisibility
): List<MonthSignalVisual> {
    val aggregation = aggregation ?: return emptyList()
    return buildList {
        if (visibility.showDiary && aggregation.hasDiary) {
            add(MonthSignalVisual(MonthWidgetSignal.DIARY, FluxWidgetTheme.accent))
        }
        if (visibility.showTodo && aggregation.pendingTodosCount > 0) {
            add(MonthSignalVisual(MonthWidgetSignal.TODO, FluxWidgetTheme.primary))
        }
        if (visibility.showEvent && aggregation.eventColors.isNotEmpty()) {
            add(MonthSignalVisual(MonthWidgetSignal.EVENT, FluxWidgetTheme.muted))
        }
        if (visibility.showTrash && aggregation.deletedCount > 0) {
            add(MonthSignalVisual(MonthWidgetSignal.TRASH, FluxWidgetTheme.danger))
        }
    }
}

@Composable
private fun MonthLegend(visibility: MonthWidgetVisibility) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(MonthWidgetSignal.DIARY, FluxWidgetTheme.accent, visibility, GlanceModifier.defaultWeight())
        LegendItem(MonthWidgetSignal.TODO, FluxWidgetTheme.primary, visibility, GlanceModifier.defaultWeight())
        LegendItem(MonthWidgetSignal.EVENT, FluxWidgetTheme.muted, visibility, GlanceModifier.defaultWeight())
        LegendItem(MonthWidgetSignal.TRASH, FluxWidgetTheme.danger, visibility, GlanceModifier.defaultWeight())
    }
}

@Composable
private fun LegendItem(
    signal: MonthWidgetSignal,
    color: androidx.glance.unit.ColorProvider,
    visibility: MonthWidgetVisibility,
    modifier: GlanceModifier
) {
    val enabled = visibility.isVisible(signal)
    val toggleAction = actionRunCallback<MonthWidgetToggleSignalAction>(
        actionParametersOf(MonthWidgetOptions.signalParameterKey to signal.parameterValue)
    )
    Row(
        modifier = modifier
            .height(28.dp)
            .clickable(toggleAction)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignalBar(if (enabled) color else FluxWidgetTheme.subtle)
        Text(
            text = signal.label,
            modifier = GlanceModifier.padding(start = 4.dp),
            style = TextStyle(
                color = if (enabled) FluxWidgetTheme.muted else FluxWidgetTheme.subtle,
                fontSize = 10.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SignalBar(color: androidx.glance.unit.ColorProvider) {
    Spacer(
        modifier = GlanceModifier
            .width(18.dp)
            .height(3.dp)
            .padding(vertical = 1.dp)
            .background(color)
    )
}

private data class MonthSignalVisual(
    val signal: MonthWidgetSignal,
    val color: androidx.glance.unit.ColorProvider
)

private val MONTH_WIDGET_FIXED_VERTICAL_SPACE = 116.dp
private val MONTH_WIDGET_MIN_ROW_HEIGHT = 28.dp
private const val DAYS_PER_WEEK = 7
private const val MIN_MONTH_WIDGET_WEEK_COUNT = 5
