package com.example.water

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class WaterAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.water_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.water_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    fun buildExactAlarmPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun scheduleReminder(triggerAtEpochMs: Long, amountMl: Int) {
        val pending = reminderPendingIntent(amountMl)
        alarmManager.cancel(pending)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMs,
                pending
            )
        } else {
            // Graceful fallback when user has not granted exact alarm permission
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMs,
                pending
            )
        }
    }

    fun cancelReminder() {
        alarmManager.cancel(reminderPendingIntent(250))
    }

    fun postHydrationNotification(amountMl: Int) {
        ensureChannel()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val openAppIntent = PendingIntent.getActivity(
            context,
            REQ_OPEN_APP,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drankIntent = PendingIntent.getBroadcast(
            context,
            REQ_DRANK_250,
            Intent(context, WaterNotificationReceiver::class.java).apply {
                action = WaterNotificationReceiver.ACTION_DRANK_250
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            REQ_SNOOZE_15,
            Intent(context, WaterNotificationReceiver::class.java).apply {
                action = WaterNotificationReceiver.ACTION_SNOOZE_15
                putExtra(EXTRA_AMOUNT_ML, amountMl)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Hydration Check · $amountMl ml")
            .setContentText("Tap below to log 250 ml or snooze 15 min without opening Kinetix.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .addAction(0, "Drank 250 ml", drankIntent)
            .addAction(0, "Snooze 15 min", snoozeIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
        }
    }

    fun postWakeUpPromptNotification() {
        ensureChannel()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val imUpIntent = PendingIntent.getBroadcast(
            context,
            REQ_IM_UP,
            Intent(context, WaterNotificationReceiver::class.java).apply {
                action = WaterNotificationReceiver.ACTION_IM_UP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Good morning — ready to hydrate?")
            .setContentText("Tap \"I'm up\" to space today's water targets until 2.5h before bed.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "I'm up", imUpIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WAKE, notification)
        }
    }

    fun dismissAllNotifications() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_REMINDER)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_WAKE)
    }

    private fun reminderPendingIntent(amountMl: Int): PendingIntent {
        val intent = Intent(context, WaterNotificationReceiver::class.java).apply {
            action = WaterNotificationReceiver.ACTION_WATER_REMINDER
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        return PendingIntent.getBroadcast(
            context,
            REQ_ALARM_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "kinetix_hydration"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        private const val NOTIFICATION_ID_REMINDER = 4101
        private const val NOTIFICATION_ID_WAKE = 4102
        private const val REQ_ALARM_REMINDER = 5001
        private const val REQ_DRANK_250 = 5002
        private const val REQ_SNOOZE_15 = 5003
        private const val REQ_IM_UP = 5004
        private const val REQ_OPEN_APP = 5005
    }
}
