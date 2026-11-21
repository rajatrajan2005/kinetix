package com.example

import com.example.data.local.entity.WorkoutSetEntity
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.TrainingGoal
import com.example.domain.model.VolumeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class KinetixCalculatorsTest {

    @Test
    fun `macro calculator applies Mifflin-St Jeor and cut-maintain-bulk adjustments`() {
        val maintain = MacroCalculator.calculate(
            age = 30,
            sex = Sex.MALE,
            heightCm = 180.0,
            weightKg = 80.0,
            activityLevel = ActivityLevel.MODERATE,
            nutritionGoal = NutritionGoal.MAINTAIN,
            proteinPerKg = 2.0
        )
        // BMR = 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780, maintain.bmrKcal)
        // TDEE = 1780 * 1.55 = 2759
        assertEquals(2759, maintain.tdeeKcal)
        assertEquals(2759, maintain.targetKcal)
        assertEquals(160, maintain.proteinG)
        assertEquals(64, maintain.fatG) // 0.8 * 80

        val cut = MacroCalculator.calculate(
            age = 30,
            sex = Sex.MALE,
            heightCm = 180.0,
            weightKg = 80.0,
            activityLevel = ActivityLevel.MODERATE,
            nutritionGoal = NutritionGoal.CUT,
            cutDeficitPct = 0.20
        )
        assertEquals((2759 * 0.80).toInt(), cut.targetKcal)

        val bulk = MacroCalculator.calculate(
            age = 30,
            sex = Sex.MALE,
            heightCm = 180.0,
            weightKg = 80.0,
            activityLevel = ActivityLevel.MODERATE,
            nutritionGoal = NutritionGoal.BULK
        )
        assertEquals(Math.round(2759 * 1.10).toInt(), bulk.targetKcal)
    }

    @Test
    fun `training tuning preserves hypertrophy volume on fat loss and triggers double progression`() {
        val hyp = TrainingTuningCalculator.prescriptionFor(TrainingGoal.HYPERTROPHY)
        val fatLoss = TrainingTuningCalculator.prescriptionFor(TrainingGoal.FAT_LOSS)
        assertEquals(hyp.sets, fatLoss.sets)
        assertEquals(hyp.repMin, fatLoss.repMin)
        assertEquals(hyp.repMax, fatLoss.repMax)

        val setsHitTop = (1..3).map { idx ->
            WorkoutSetEntity(
                sessionId = 1,
                exerciseId = "ex_1",
                exerciseName = "Bench Press",
                muscleGroup = "Chest",
                muscleHead = "Mid Chest",
                setNumber = idx,
                weightKg = 80.0,
                reps = 12,
                completedAtEpochMs = 1000L
            )
        }
        val suggestion = TrainingTuningCalculator.evaluateDoubleProgression(
            previousSessionSets = setsHitTop,
            targetSets = 3,
            repMax = 12
        )
        assertNotNull(suggestion)
        assertTrue(suggestion!!.shouldIncrease)
        assertEquals(82.5, suggestion.suggestedWeightKg, 0.01)

        val setsNotReady = setsHitTop.dropLast(1) + setsHitTop.last().copy(reps = 9)
        val hold = TrainingTuningCalculator.evaluateDoubleProgression(
            previousSessionSets = setsNotReady,
            targetSets = 3,
            repMax = 12
        )
        assertFalse(hold!!.shouldIncrease)
        assertEquals(80.0, hold.suggestedWeightKg, 0.01)
    }

    @Test
    fun `weekly volume flags under 10 and over 20 hard sets`() {
        val chestSets = (1..12).map {
            WorkoutSetEntity(
                sessionId = 1, exerciseId = "c", exerciseName = "Press",
                muscleGroup = "Chest", muscleHead = "Mid Chest", setNumber = it,
                weightKg = 60.0, reps = 10, completedAtEpochMs = 1L
            )
        }
        val bicepsSets = (1..22).map {
            WorkoutSetEntity(
                sessionId = 1, exerciseId = "b", exerciseName = "Curl",
                muscleGroup = "Biceps", muscleHead = "Long Head", setNumber = it,
                weightKg = 15.0, reps = 12, completedAtEpochMs = 1L
            )
        }
        val analyzed = TrainingTuningCalculator.analyzeWeeklyVolume(chestSets + bicepsSets)
        assertEquals(VolumeStatus.OPTIMAL, analyzed.first { it.muscleGroup == "Chest" }.status)
        assertEquals(VolumeStatus.EXCESS, analyzed.first { it.muscleGroup == "Biceps" }.status)
        assertEquals(VolumeStatus.UNDER, analyzed.first { it.muscleGroup == "Back" }.status)
    }

    @Test
    fun `water schedule caps doses at 600ml and handles late wake catch up`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 21)
        val morningNow = today.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val normalSlots = WaterScheduleCalculator.buildRemainingSchedule(
            date = today,
            wakeTime = LocalTime.of(7, 0),
            bedTime = LocalTime.of(23, 0),
            nowEpochMs = morningNow,
            targetMl = 2800,
            consumedMl = 0,
            zoneId = zone
        )
        assertTrue(normalSlots.isNotEmpty())
        assertTrue(normalSlots.all { it.amountMl <= WaterScheduleCalculator.MAX_DOSE_ML })
        assertEquals(2800, normalSlots.sumOf { it.amountMl })

        // Late wake: 18:30 with bedtime 22:00 (cutoff 19:30 -> 60 mins left)
        val lateNow = today.atTime(18, 30).atZone(zone).toInstant().toEpochMilli()
        val lateSlots = WaterScheduleCalculator.buildRemainingSchedule(
            date = today,
            wakeTime = LocalTime.of(18, 30),
            bedTime = LocalTime.of(22, 0),
            nowEpochMs = lateNow,
            targetMl = 2500,
            consumedMl = 0,
            zoneId = zone
        )
        assertTrue(lateSlots.isNotEmpty())
        assertTrue(lateSlots.all { it.amountMl <= WaterScheduleCalculator.MAX_DOSE_ML })
        assertTrue(lateSlots.any { it.isCatchUp })
    }
}
