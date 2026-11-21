package com.example.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hardware-batched TYPE_STEP_COUNTER reader (near-zero battery, no continuous accelerometer polling).
 */
class StepSensorManager(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun isSensorAvailable(): Boolean = stepSensor != null

    /**
     * Streams hardware step counter updates while collected (foreground UI),
     * utilizing 5-second maxReportLatencyUs hardware FIFO batching.
     */
    fun observeHardwareCounter(): Flow<Long> = callbackFlow {
        val sensor = stepSensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.firstOrNull()?.toLong() ?: return
                trySend(value)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            5_000_000
        )
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /**
     * One-shot read used by periodic WorkManager sync without keeping a foreground service alive.
     */
    suspend fun readSingleHardwareCount(timeoutMs: Long = 3_000L): Long? {
        val sensor = stepSensor ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val value = event?.values?.firstOrNull()?.toLong() ?: return
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) cont.resume(value)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                val registered = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                if (!registered && cont.isActive) {
                    cont.resume(null)
                }
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }
}
