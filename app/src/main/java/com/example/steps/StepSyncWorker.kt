package com.example.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.di.AppContainer

class StepSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val container = AppContainer.get(applicationContext)
        val count = container.stepSensorManager.readSingleHardwareCount() ?: return Result.success()
        container.stepRepository.recordHardwareCounter(count)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "kinetix_step_sync"
    }
}
