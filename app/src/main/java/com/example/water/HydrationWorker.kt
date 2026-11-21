package com.example.water

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.di.AppContainer
import java.time.LocalDate
import java.time.LocalTime

/**
 * Periodic WorkManager check ensuring wake-up prompt is offered if user hasn't opened the app yet,
 * and recalculating next exact alarm if day rolled over.
 */
class HydrationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.get(applicationContext)
        val today = LocalDate.now().toEpochDay()
        val state = container.waterRepository.getScheduleStateOnce(today)
        val nowTime = LocalTime.now()

        if (state == null && nowTime.hour in 6..12) {
            container.waterAlarmScheduler.postWakeUpPromptNotification()
        } else {
            container.waterRepository.rescheduleFromCurrentState()
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "kinetix_hydration_sync"
    }
}
