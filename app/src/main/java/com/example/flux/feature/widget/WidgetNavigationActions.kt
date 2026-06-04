package com.example.flux.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.example.flux.MainActivity
import com.example.flux.app.navigation.AppDestinations
import com.example.flux.app.navigation.AppLaunchIntent

fun openDestinationAction(context: Context, destination: AppDestinations): Action {
    val destinationName = destination.name
    val intent = Intent(AppLaunchIntent.ACTION_OPEN_DESTINATION).apply {
        setClass(context, MainActivity::class.java)
        data = Uri.parse("flux://widget/open/$destinationName")
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(AppLaunchIntent.EXTRA_DESTINATION, destinationName)
    }
    return actionStartActivity(intent)
}
