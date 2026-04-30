package com.example.flux.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxWidgetEntryPoint::class.java
        )
        val state = entryPoint.calendarAggregatorUseCase().invoke().first().toMonthWidgetState()

        provideContent {
            MonthWidgetContent(state)
        }
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

@Composable
private fun MonthWidgetContent(state: MonthWidgetState) {
    Column(
        modifier = GlanceModifier
            .widgetContainer()
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            text = state.title,
            style = TextStyle(
                color = FluxWidgetTheme.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        WeekHeader()
        state.cells.chunked(7).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                week.forEach { cell ->
                    MonthDayCell(cell, modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
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
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun MonthDayCell(cell: MonthWidgetDayCell, modifier: GlanceModifier = GlanceModifier) {
    val contentColor = if (cell.isToday) FluxWidgetTheme.primary else FluxWidgetTheme.text
    Column(
        modifier = modifier
            .height(32.dp)
            .padding(1.dp)
            .background(if (cell.isToday) FluxWidgetTheme.surface else FluxWidgetTheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = cell.day?.toString().orEmpty(),
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal
            )
        )
        if (cell.aggregation != null) {
            Row {
                if (cell.aggregation.hasDiary) Dot(FluxWidgetTheme.accent)
                if (cell.aggregation.pendingTodosCount > 0) Dot(FluxWidgetTheme.primary)
                if (cell.aggregation.eventColors.isNotEmpty()) Dot(FluxWidgetTheme.muted)
                if (cell.aggregation.deletedCount > 0) Dot(FluxWidgetTheme.danger)
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.glance.unit.ColorProvider) {
    Spacer(
        modifier = GlanceModifier
            .width(4.dp)
            .height(4.dp)
            .padding(horizontal = 1.dp)
            .background(color)
    )
}
