package com.example.data.repository

import com.example.data.local.dao.StepDao
import com.example.data.local.entity.StepRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.max

/**
 * Persists daily steps in Room while handling hardware TYPE_STEP_COUNTER resets across device reboots.
 */
class StepRepository(
    private val stepDao: StepDao
) {
    fun observeTodaySteps(epochDay: Long = LocalDate.now().toEpochDay()): Flow<StepRecordEntity?> =
        stepDao.observeStepRecord(epochDay)

    fun observeRecentSteps(): Flow<List<StepRecordEntity>> =
        stepDao.observeRecentSteps()

    suspend fun recordHardwareCounter(
        hardwareCounterSinceBoot: Long,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): StepRecordEntity {
        val safeHardware = max(0L, hardwareCounterSinceBoot)
        val now = System.currentTimeMillis()
        val existing = stepDao.getStepRecordOnce(epochDay)

        val updated = if (existing == null) {
            // First reading of the day establishes baseline offset
            StepRecordEntity(
                dateEpochDay = epochDay,
                stepsToday = 0L,
                lastHardwareCounter = safeHardware,
                hardwareOffset = safeHardware,
                updatedAtEpochMs = now
            )
        } else if (safeHardware < existing.lastHardwareCounter) {
            // Device rebooted: hardware counter restarted from 0, preserve accumulated stepsToday
            val newTotal = existing.stepsToday + safeHardware
            StepRecordEntity(
                dateEpochDay = epochDay,
                stepsToday = newTotal,
                lastHardwareCounter = safeHardware,
                hardwareOffset = 0L,
                updatedAtEpochMs = now
            )
        } else {
            val delta = safeHardware - existing.lastHardwareCounter
            StepRecordEntity(
                dateEpochDay = epochDay,
                stepsToday = existing.stepsToday + delta,
                lastHardwareCounter = safeHardware,
                hardwareOffset = existing.hardwareOffset,
                updatedAtEpochMs = now
            )
        }

        stepDao.upsertStepRecord(updated)
        return updated
    }
}
