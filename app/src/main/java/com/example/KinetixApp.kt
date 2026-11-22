package com.example

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.di.AppContainer
import com.example.steps.StepSyncWorker
import com.example.water.HydrationWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Minimal Application class: zero synchronous DB/network/WorkManager work on main thread
 * during onCreate() to guarantee <500 ms cold startup.
 */
class KinetixApp : Application(), Configuration.Provider {

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.WARN)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val container = AppContainer.get(this@KinetixApp)
            container.workoutRepository.ensureSportExercisesSeeded()
            container.workoutRepository.ensureStarterSplitIfEmpty()
            container.waterRepository.onFirstAppOpenToday()
            enqueueBackgroundWork()
        }
    }

    private fun enqueueBackgroundWork() {
        runCatching {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
            val wm = WorkManager.getInstance(this)
            val stepRequest = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                StepSyncWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                stepRequest
            )

            val hydrationRequest = PeriodicWorkRequestBuilder<HydrationWorker>(6, TimeUnit.HOURS)
                .build()
            wm.enqueueUniquePeriodicWork(
                HydrationWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                hydrationRequest
            )
        }
    }
}
