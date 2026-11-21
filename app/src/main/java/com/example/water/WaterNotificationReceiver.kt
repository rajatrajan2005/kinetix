package com.example.water

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer.get(appContext)
                val waterRepo = container.waterRepository
                val scheduler = container.waterAlarmScheduler

                when (action) {
                    ACTION_WATER_REMINDER -> {
                        val amount = intent.getIntExtra(WaterAlarmScheduler.EXTRA_AMOUNT_ML, 250)
                        scheduler.postHydrationNotification(amount)
                        waterRepo.rescheduleFromCurrentState()
                    }
                    ACTION_DRANK_250 -> {
                        waterRepo.logWater(250, source = "notification_action")
                        scheduler.dismissAllNotifications()
                    }
                    ACTION_SNOOZE_15 -> {
                        val amount = intent.getIntExtra(WaterAlarmScheduler.EXTRA_AMOUNT_ML, 250)
                        waterRepo.snoozeMinutes(15, amount)
                        scheduler.dismissAllNotifications()
                    }
                    ACTION_IM_UP -> {
                        waterRepo.markWakeUpNow(manualOverride = false)
                        scheduler.dismissAllNotifications()
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        waterRepo.rescheduleFromCurrentState()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WATER_REMINDER = "com.example.action.WATER_REMINDER"
        const val ACTION_DRANK_250 = "com.example.action.DRANK_250"
        const val ACTION_SNOOZE_15 = "com.example.action.SNOOZE_15"
        const val ACTION_IM_UP = "com.example.action.IM_UP"
    }
}
