package com.example.domain.calculator

import androidx.compose.runtime.Immutable
import com.example.data.local.entity.CycleLogEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class CyclePhase(val label: String, val trainingNote: String) {
    MENSTRUAL("Menstrual", "Prioritize gentle volume, hydration, and recovery sleep."),
    FOLLICULAR("Follicular", "Rising energy & strength window — great time to push progressive overload."),
    OVULATORY("Ovulatory", "Peak power output window — focus on clean technique & warm-up."),
    LUTEAL("Luteal", "Higher core temp & fatigue — stay consistent with protein, water, and moderate RPE.")
}

@Immutable
data class CycleSummary(
    val cycleDay: Int,
    val phase: CyclePhase,
    val daysUntilNextPeriod: Int,
    val predictedNextStartEpochDay: Long,
    val predictedNextDateLabel: String,
    val isCurrentlyPeriod: Boolean,
    val lastLoggedStartEpochDay: Long?
)

object CycleCalculator {

    private val dateFmt = DateTimeFormatter.ofPattern("MMM d")

    fun summarize(
        logs: List<CycleLogEntity>,
        cycleLengthDays: Int,
        periodLengthDays: Int,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): CycleSummary? {
        val latest = logs.maxByOrNull { it.startDateEpochDay } ?: return null
        val safeCycleLen = cycleLengthDays.coerceIn(20, 45)
        val safePeriodLen = periodLengthDays.coerceIn(2, 10)

        val rawElapsed = (todayEpochDay - latest.startDateEpochDay).toInt().coerceAtLeast(0)
        val cycleDay = (rawElapsed % safeCycleLen) + 1
        val cyclesElapsed = rawElapsed / safeCycleLen
        val predictedNext = latest.startDateEpochDay + ((cyclesElapsed + 1L) * safeCycleLen)
        val daysUntil = (predictedNext - todayEpochDay).toInt().coerceAtLeast(0)

        val ovulationDay = (safeCycleLen - 14).coerceIn(safePeriodLen + 2, safeCycleLen - 4)
        val phase = when {
            cycleDay <= safePeriodLen -> CyclePhase.MENSTRUAL
            cycleDay < ovulationDay - 1 -> CyclePhase.FOLLICULAR
            cycleDay <= ovulationDay + 1 -> CyclePhase.OVULATORY
            else -> CyclePhase.LUTEAL
        }

        val isOpenPeriod = latest.endDateEpochDay == null && rawElapsed < safePeriodLen
        val isWithinLoggedRange = latest.endDateEpochDay != null &&
            todayEpochDay in latest.startDateEpochDay..latest.endDateEpochDay

        return CycleSummary(
            cycleDay = cycleDay,
            phase = phase,
            daysUntilNextPeriod = daysUntil,
            predictedNextStartEpochDay = predictedNext,
            predictedNextDateLabel = LocalDate.ofEpochDay(predictedNext).format(dateFmt),
            isCurrentlyPeriod = isOpenPeriod || isWithinLoggedRange,
            lastLoggedStartEpochDay = latest.startDateEpochDay
        )
    }
}
