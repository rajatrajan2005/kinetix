package com.example.domain.calculator

import com.example.domain.model.WaterDoseSlot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Distributes daily water target (30-35 ml/kg, default 33 ml/kg) across the window
 * from wake time to (bedtime - 2.5 h) at equal intervals, capped at 600 ml per dose.
 *
 * If a late wake leaves too little time to deliver the remaining target within the
 * 600 ml cap at safe spacing (>= 45 min), schedules safe doses and routes any overflow
 * into a smaller gentle catch-up dose rather than an oversized bolus.
 */
object WaterScheduleCalculator {

    const val MAX_DOSE_ML = 600
    const val MIN_DOSE_ML = 150
    const val MIN_SPACING_MINUTES = 45L
    const val BEDTIME_CUTOFF_MINUTES = 150L // 2.5 hours

    fun dailyTargetMl(weightKg: Double, mlPerKg: Double = 33.0): Int {
        val safeWeight = weightKg.coerceIn(35.0, 250.0)
        val safeFactor = mlPerKg.coerceIn(30.0, 35.0)
        return ((safeWeight * safeFactor) / 50.0).roundToInt() * 50
    }

    fun buildRemainingSchedule(
        date: LocalDate,
        wakeTime: LocalTime,
        bedTime: LocalTime,
        nowEpochMs: Long,
        targetMl: Int,
        consumedMl: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<WaterDoseSlot> {
        val remainingMl = max(0, targetMl - consumedMl)
        if (remainingMl == 0) return emptyList()

        val wakeEpochMs = date.atTime(wakeTime).atZone(zoneId).toInstant().toEpochMilli()
        val bedDate = if (bedTime.isBefore(wakeTime) || bedTime == wakeTime) date.plusDays(1) else date
        val bedEpochMs = bedDate.atTime(bedTime).atZone(zoneId).toInstant().toEpochMilli()
        val cutoffEpochMs = bedEpochMs - (BEDTIME_CUTOFF_MINUTES * 60_000L)

        val windowStartMs = max(wakeEpochMs, nowEpochMs + 10 * 60_000L)
        if (windowStartMs >= cutoffEpochMs) {
            // Very late in the day: single modest catch-up sip capped at 250 ml before cutoff
            val gentleSip = min(remainingMl, 250)
            return listOf(
                WaterDoseSlot(
                    triggerEpochMs = max(nowEpochMs + 5 * 60_000L, cutoffEpochMs - 15 * 60_000L),
                    amountMl = gentleSip,
                    isCatchUp = true
                )
            )
        }

        val availableMinutes = (cutoffEpochMs - windowStartMs) / 60_000L
        val maxSlotsBySpacing = max(1, (availableMinutes / MIN_SPACING_MINUTES).toInt() + 1)
        val minSlotsNeededForCap = ceil(remainingMl.toDouble() / MAX_DOSE_ML).toInt()

        val slots = mutableListOf<WaterDoseSlot>()

        if (minSlotsNeededForCap <= maxSlotsBySpacing) {
            // Enough time: choose balanced count (aim ~300-450ml per slot, <= 600ml)
            val idealSlotCount = max(
                minSlotsNeededForCap,
                min(maxSlotsBySpacing, ceil(remainingMl / 350.0).toInt())
            )
            val baseDose = (remainingMl / idealSlotCount / 25) * 25
            val intervalMs = if (idealSlotCount == 1) 0L else (cutoffEpochMs - windowStartMs) / idealSlotCount

            var allocated = 0
            for (i in 0 until idealSlotCount) {
                val dose = if (i == idealSlotCount - 1) {
                    min(MAX_DOSE_ML, remainingMl - allocated)
                } else {
                    baseDose.coerceIn(MIN_DOSE_ML, MAX_DOSE_ML)
                }
                allocated += dose
                val triggerMs = windowStartMs + (intervalMs * i)
                slots.add(
                    WaterDoseSlot(
                        triggerEpochMs = triggerMs,
                        amountMl = dose,
                        isCatchUp = false
                    )
                )
            }
        } else {
            // Late wake compressed window: fill available slots capped at 600 ml,
            // and schedule a smaller gentle catch-up dose instead of exceeding 600 ml.
            val intervalMs = if (maxSlotsBySpacing <= 1) 0L else (cutoffEpochMs - windowStartMs) / maxSlotsBySpacing
            val regularSlots = max(1, maxSlotsBySpacing - 1)
            var allocated = 0
            for (i in 0 until regularSlots) {
                val dose = min(MAX_DOSE_ML, remainingMl - allocated)
                if (dose <= 0) break
                allocated += dose
                slots.add(
                    WaterDoseSlot(
                        triggerEpochMs = windowStartMs + (intervalMs * i),
                        amountMl = dose,
                        isCatchUp = false
                    )
                )
            }
            val leftover = remainingMl - allocated
            if (leftover > 0) {
                // Smaller gentle catch-up rather than an oversized bolus
                val safeCatchUp = min(leftover, 300)
                slots.add(
                    WaterDoseSlot(
                        triggerEpochMs = cutoffEpochMs - 15 * 60_000L,
                        amountMl = safeCatchUp,
                        isCatchUp = true
                    )
                )
            }
        }

        return slots.sortedBy { it.triggerEpochMs }
    }

    fun formatTime(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalTime()
}
