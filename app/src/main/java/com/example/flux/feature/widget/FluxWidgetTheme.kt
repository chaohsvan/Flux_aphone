package com.example.flux.feature.widget

import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import com.example.flux.R

object FluxWidgetTheme {
    val background = ColorProvider(R.color.widget_background)
    val surface = ColorProvider(R.color.widget_surface)
    val primary = ColorProvider(R.color.widget_primary)
    val accent = ColorProvider(R.color.widget_accent)
    val danger = ColorProvider(R.color.widget_danger)
    val text = ColorProvider(R.color.widget_text)
    val muted = ColorProvider(R.color.widget_muted)
    val subtle = ColorProvider(R.color.widget_subtle)
}

fun GlanceModifier.widgetContainer(): GlanceModifier {
    return fillMaxSize()
        .background(FluxWidgetTheme.background)
        .padding(14.dp)
}
