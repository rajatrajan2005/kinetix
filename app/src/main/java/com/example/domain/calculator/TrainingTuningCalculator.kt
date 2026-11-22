package com.example.domain.calculator

import com.example.data.local.entity.WorkoutSetEntity
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.OverloadSuggestion
import com.example.domain.model.SetRepPrescription
import com.example.domain.model.TrainingGoal
import com.example.domain.model.VolumeStatus
import kotlin.math.round

import com.example.domain.model.NutritionGoal

@androidx.compose.runtime.Immutable
data class GoalBiometricTargets(
    val nutritionGoal: NutritionGoal,
    val proteinPerKg: Double,
    val waterMlPerKg: Double,
    val stepGoal: Int,
    val summaryNote: String
)

/**
 * Computes goal-based set/rep prescriptions, goal-aligned protein/water/step targets,
 * double-progression overload suggestions, and weekly hard-set muscle volume classifications.
 */
object TrainingTuningCalculator {

    private const val PROGRESSION_STEP_KG = 2.5

    fun recommendedTargetsFor(goal: TrainingGoal): GoalBiometricTargets = when (goal) {
        TrainingGoal.STRENGTH -> GoalBiometricTargets(
            nutritionGoal = NutritionGoal.BULK,
            proteinPerKg = 1.9,
            waterMlPerKg = 32.0,
            stepGoal = 7500,
            summaryNote = "Surplus +10% · 1.9 g/kg protein · 32 ml/kg water · 7.5k steps"
        )
        TrainingGoal.HYPERTROPHY -> GoalBiometricTargets(
            nutritionGoal = NutritionGoal.BULK,
            proteinPerKg = 2.0,
            waterMlPerKg = 33.0,
            stepGoal = 8500,
            summaryNote = "Lean gain +10% · 2.0 g/kg protein · 33 ml/kg water · 8.5k steps"
        )
        TrainingGoal.BODY_RECOMP -> GoalBiometricTargets(
            nutritionGoal = NutritionGoal.RECOMP,
            proteinPerKg = 2.1,
            waterMlPerKg = 35.0,
            stepGoal = 9500,
            summaryNote = "Recomp (~97% TDEE) · 2.1 g/kg protein · 35 ml/kg water · 9.5k steps"
        )
        TrainingGoal.FAT_LOSS -> GoalBiometricTargets(
            nutritionGoal = NutritionGoal.CUT,
            proteinPerKg = 2.2,
            waterMlPerKg = 35.0,
            stepGoal = 11000,
            summaryNote = "Cut (-17.5% TDEE) · 2.2 g/kg protein · 35 ml/kg water · 11k steps"
        )
        TrainingGoal.ENDURANCE -> GoalBiometricTargets(
            nutritionGoal = NutritionGoal.MAINTAIN,
            proteinPerKg = 1.7,
            waterMlPerKg = 35.0,
            stepGoal = 12000,
            summaryNote = "High-carb maintenance · 1.7 g/kg protein · 35 ml/kg water · 12k steps"
        )
    }

    fun prescriptionFor(goal: TrainingGoal): SetRepPrescription = when (goal) {
        TrainingGoal.STRENGTH -> SetRepPrescription(
            sets = 4,
            repMin = 3,
            repMax = 6,
            restSeconds = 180,
            note = "Heavy compound focus · long rest"
        )
        TrainingGoal.HYPERTROPHY -> SetRepPrescription(
            sets = 3,
            repMin = 6,
            repMax = 12,
            restSeconds = 90,
            note = "Mechanical tension · controlled eccentric"
        )
        TrainingGoal.BODY_RECOMP -> SetRepPrescription(
            sets = 3,
            repMin = 8,
            repMax = 12,
            restSeconds = 90,
            note = "Build lean mass & drop fat · tight progressive overload"
        )
        TrainingGoal.FAT_LOSS -> SetRepPrescription(
            sets = 3,
            repMin = 6,
            repMax = 12,
            restSeconds = 90,
            note = "Hypertrophy stimulus preserved · diet drives deficit"
        )
        TrainingGoal.ENDURANCE -> SetRepPrescription(
            sets = 3,
            repMin = 12,
            repMax = 20,
            restSeconds = 60,
            note = "Metabolic conditioning · short rest"
        )
    }

    /**
     * Double progression: when ALL working sets in the previous session reached
     * at least [repMax] reps at the working weight, recommend +2.5 kg.
     */
    fun evaluateDoubleProgression(
        previousSessionSets: List<WorkoutSetEntity>,
        targetSets: Int,
        repMax: Int
    ): OverloadSuggestion? {
        val workingSets = previousSessionSets.filter { !it.isWarmup && it.reps > 0 }
        if (workingSets.isEmpty()) return null

        val lastTopWeight = workingSets.maxOf { it.weightKg }
        val completedEnoughSets = workingSets.size >= targetSets
        val allHitTop = workingSets.all { it.reps >= repMax }

        return if (completedEnoughSets && allHitTop) {
            val nextWeight = roundToHalfKg(lastTopWeight + PROGRESSION_STEP_KG)
            OverloadSuggestion(
                shouldIncrease = true,
                suggestedWeightKg = nextWeight,
                reason = "All ${workingSets.size} sets hit $repMax+ reps -> +2.5 kg"
            )
        } else {
            OverloadSuggestion(
                shouldIncrease = false,
                suggestedWeightKg = lastTopWeight,
                reason = "Match ${lastTopWeight} kg and push toward $repMax reps"
            )
        }
    }

    fun analyzeWeeklyVolume(
        setsLast7Days: List<WorkoutSetEntity>,
        trackedMuscles: List<String> = DEFAULT_MUSCLE_GROUPS
    ): List<MuscleWeeklyVolume> {
        val countsByMuscle = setsLast7Days
            .asSequence()
            .filter { it.isHardSet && !it.isWarmup }
            .groupingBy { normalizeMuscleGroup(it.muscleGroup) }
            .eachCount()

        val allMuscles = (trackedMuscles + countsByMuscle.keys).distinct()
        return allMuscles.map { muscle ->
            val sets = countsByMuscle[muscle] ?: 0
            val status = when {
                sets < 10 -> VolumeStatus.UNDER
                sets > 20 -> VolumeStatus.EXCESS
                else -> VolumeStatus.OPTIMAL
            }
            MuscleWeeklyVolume(
                muscleGroup = muscle,
                hardSets = sets,
                status = status
            )
        }
    }

    fun normalizeMuscleGroup(raw: String): String = when (raw.trim().lowercase()) {
        "pectorals", "chest" -> "Chest"
        "lats", "upper back", "middle back", "lower back", "traps", "back" -> "Back"
        "shoulders", "delts", "deltoids" -> "Shoulders"
        "biceps" -> "Biceps"
        "triceps" -> "Triceps"
        "quadriceps", "quads" -> "Quads"
        "hamstrings" -> "Hamstrings"
        "glutes" -> "Glutes"
        "calves" -> "Calves"
        "abdominals", "abs", "core" -> "Core"
        else -> raw.trim().replaceFirstChar { it.uppercase() }
    }

    private fun roundToHalfKg(value: Double): Double = round(value * 2.0) / 2.0

    val DEFAULT_MUSCLE_GROUPS = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Quads", "Hamstrings", "Glutes", "Calves", "Core"
    )
}
