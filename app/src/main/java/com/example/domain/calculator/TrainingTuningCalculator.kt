package com.example.domain.calculator

import com.example.data.local.entity.WorkoutSetEntity
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.OverloadSuggestion
import com.example.domain.model.SetRepPrescription
import com.example.domain.model.TrainingGoal
import com.example.domain.model.VolumeStatus
import kotlin.math.round

/**
 * Computes goal-based set/rep prescriptions, double-progression overload suggestions,
 * and weekly hard-set muscle volume classifications without mutating the user's split.
 */
object TrainingTuningCalculator {

    private const val PROGRESSION_STEP_KG = 2.5

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
