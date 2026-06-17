package com.example.flux.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.core.database.FluxDatabase
import com.example.flux.core.settings.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, -1L)
        if (id.isBlank() || type.isBlank() || triggerAtMillis <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleReminder(context.applicationContext, type, id, triggerAtMillis)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(
        context: Context,
        type: String,
        id: String,
        triggerAtMillis: Long
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderReceiverEntryPoint::class.java
        )
        val database = entryPoint.fluxDatabase()
        val reminder = currentReminder(database, type, id, triggerAtMillis) ?: return

        if (canPostNotifications(context)) {
            withContext(Dispatchers.Main) {
                postNotification(context, type, id, reminder.title, reminder.message)
            }
        }

        if (type == ReminderContract.TYPE_EVENT) {
            database.eventDao().getEventById(id)?.let { entryPoint.reminderScheduler().scheduleEvent(it) }
        }
    }

    private suspend fun currentReminder(
        database: FluxDatabase,
        type: String,
        id: String,
        triggerAtMillis: Long
    ): ReminderPlan? {
        return when (type) {
            ReminderContract.TYPE_DIARY -> database.diaryDao().getDiaryById(id)
                ?.let { ReminderPlanner.diaryPlanAtTrigger(it, triggerAtMillis) }
            ReminderContract.TYPE_TODO -> database.todoDao().getTodoById(id)
                ?.let { ReminderPlanner.todoPlanAtTrigger(it, triggerAtMillis) }
            ReminderContract.TYPE_EVENT -> database.eventDao().getEventById(id)
                ?.let { ReminderPlanner.eventPlanAtTrigger(it, triggerAtMillis) }
            else -> null
        }
    }

    private fun postNotification(
        context: Context,
        type: String,
        id: String,
        title: String,
        message: String
    ) {
        val soundEnabled = AppPreferences(context).isReminderSoundEnabled()
        val channelId = if (soundEnabled) CHANNEL_ID_SOUND else CHANNEL_ID_SILENT

        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            "$type:$id".hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(!soundEnabled)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify("$type:$id".hashCode(), notification)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val soundChannel = NotificationChannel(
            CHANNEL_ID_SOUND,
            "Flux \u63d0\u9192",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "\u65e5\u8bb0\u3001\u5f85\u529e\u548c\u65e5\u5386\u4e8b\u4ef6\u63d0\u9192"
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        val silentChannel = NotificationChannel(
            CHANNEL_ID_SILENT,
            "Flux \u63d0\u9192\uff08\u9759\u97f3\uff09",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "\u65e5\u8bb0\u3001\u5f85\u529e\u548c\u65e5\u5386\u4e8b\u4ef6\u9759\u97f3\u63d0\u9192"
            setSound(null, null)
        }
        manager.createNotificationChannel(soundChannel)
        manager.createNotificationChannel(silentChannel)
    }

    companion object {
        private const val CHANNEL_ID_SOUND = "flux_reminders"
        private const val CHANNEL_ID_SILENT = "flux_reminders_silent"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_ID = "id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_TRIGGER_AT_MILLIS = "trigger_at_millis"

        fun intent(
            context: Context,
            type: String,
            id: String,
            title: String,
            message: String,
            triggerAtMillis: Long = -1L
        ): Intent {
            return Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderReceiverEntryPoint {
    fun fluxDatabase(): FluxDatabase
    fun reminderScheduler(): ReminderScheduler
}
