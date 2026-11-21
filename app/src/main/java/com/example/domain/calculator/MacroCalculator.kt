package com.example.domain.calculator

import com.example.domain.model.ActivityLevel
import com.example.domain.model.MacroTargets
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure deterministic calculator for Mifflin-St Jeor BMR, TDEE, and daily macro targets.
 * - Cut: TDEE - 15% to 20% (configurable via cutDeficitPct, default 0.175)
 * - Maintain: TDEE
 * - Bulk: TDEE + 10%
 * - Protein: 1.6 - 2.2 g/kg bodyweight (default 2.0)
 * - Fat: >= 0.8 g/kg bodyweight
 * - Carbs: remaining calories / 4
 */
object MacroCalculator {

    fun calculate(
        age: Int,
        sex: Sex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        nutritionGoal: NutritionGoal,
        cutDeficitPct: Double = 0.175,
        proteinPerKg: Double = 2.0
    ): MacroTargets {
        val safeAge = age.coerceIn(14, 100)
        val safeHeight = heightCm.coerceIn(120.0, 240.0)
        val safeWeight = weightKg.coerceIn(35.0, 250.0)
        val safeProteinPerKg = proteinPerKg.coerceIn(1.6, 2.2)
        val safeCutDeficit = cutDeficitPct.coerceIn(0.15, 0.20)

        // Mifflin-St Jeor equation
        val sexOffset = if (sex == Sex.MALE) 5.0 else -161.0
        val bmr = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge) + sexOffset
        val tdee = bmr * activityLevel.factor

        val adjustedKcal = when (nutritionGoal) {
            NutritionGoal.CUT -> tdee * (1.0 - safeCutDeficit)
            NutritionGoal.MAINTAIN -> tdee
            NutritionGoal.BULK -> tdee * 1.10
        }

        val targetKcal = max(1200, adjustedKcal.roundToInt())
        val proteinG = (safeWeight * safeProteinPerKg).roundToInt()
        val fatG = (safeWeight * 0.8).roundToInt()

        val proteinAndFatKcal = (proteinG * 4) + (fatG * 9)
        val remainingKcal = max(0, targetKcal - proteinAndFatKcal)
        val carbsG = (remainingKcal / 4.0).roundToInt()

        return MacroTargets(
            bmrKcal = bmr.roundToInt(),
            tdeeKcal = tdee.roundToInt(),
            targetKcal = targetKcal,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG
        )
    }
}
