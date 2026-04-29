package com.example.flux.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flux.MainActivity
import com.example.flux.R
import com.example.flux.core.settings.AppPreferences

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Flux 提醒" }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty().ifBlank { "有一条待处理事项" }

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

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val soundChannel = NotificationChannel(
            CHANNEL_ID_SOUND,
            "Flux \u63d0\u9192",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "\u5f85\u529e\u548c\u65e5\u5386\u4e8b\u4ef6\u63d0\u9192"
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
            description = "\u5f85\u529e\u548c\u65e5\u5386\u4e8b\u4ef6\u9759\u97f3\u63d0\u9192"
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

        fun intent(
            context: Context,
            type: String,
            id: String,
            title: String,
            message: String
        ): Intent {
            return Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
            }
        }
    }
}
