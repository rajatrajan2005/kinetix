package com.example.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["muscleGroup"]),
        Index(value = ["muscleGroup", "muscleHead"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val muscleHead: String,
    val secondaryMuscles: String,
    val equipment: String,
    val level: String,
    val mechanic: String,
    val instructions: String,
    val isCustom: Int = 0
)

@Immutable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int = 27,
    val sex: String = "MALE",
    val heightCm: Double = 178.0,
    val weightKg: Double = 76.0,
    val activityLevel: String = "MODERATE",
    val nutritionGoal: String = "MAINTAIN",
    val trainingGoal: String = "HYPERTROPHY",
    val cutDeficitPct: Double = 0.175,
    val proteinPerKg: Double = 2.0,
    val wakeTimeMinutes: Int = 7 * 60,
    val bedTimeMinutes: Int = 23 * 60,
    val manualWakeOverride: Int = 0,
    val waterMlPerKg: Double = 33.0,
    val stepGoal: Int = 9000,
    val hasCompletedOnboarding: Int = 0
)

@Immutable
@Entity(tableName = "split_days")
data class SplitDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dayOrder: Int,
    val targetMusclesCsv: String
)

@Immutable
@Entity(
    tableName = "split_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SplitDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["splitDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["splitDayId"])]
)
data class SplitExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitDayId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val muscleHead: String,
    val sortOrder: Int
)

@Immutable
@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["startedAtEpochMs"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitDayId: Long,
    val splitDayName: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val isActive: Int = 1,
    val trainingGoalSnapshot: String
)

@Immutable
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId", "completedAtEpochMs"]),
        Index(value = ["muscleGroup", "completedAtEpochMs"])
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val muscleHead: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val isHardSet: Boolean = true,
    val isWarmup: Boolean = false,
    val completedAtEpochMs: Long,
    val isAdHoc: Boolean = false
)

@Immutable
@Entity(
    tableName = "food_item_cache",
    indices = [Index(value = ["name"]), Index(value = ["lastUsedAtEpochMs"])]
)
data class FoodItemCacheEntity(
    @PrimaryKey val barcodeOrKey: String,
    val name: String,
    val brand: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingSizeG: Double = 100.0,
    val cachedAtEpochMs: Long,
    val lastUsedAtEpochMs: Long = 0L,
    val isCustom: Int = 0
)

@Immutable
@Entity(
    tableName = "food_logs",
    indices = [Index(value = ["dateEpochDay"])]
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val foodKey: String,
    val foodName: String,
    val grams: Double,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val mealSlot: String,
    val loggedAtEpochMs: Long
)

@Immutable
@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["dateEpochDay"])]
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val amountMl: Int,
    val loggedAtEpochMs: Long,
    val source: String
)

@Immutable
@Entity(tableName = "water_schedule_state")
data class WaterScheduleStateEntity(
    @PrimaryKey val dateEpochDay: Long,
    val wakeMinutes: Int,
    val bedMinutes: Int,
    val targetMl: Int,
    val nextReminderAtEpochMs: Long?,
    val nextReminderAmountMl: Int,
    val snoozedUntilEpochMs: Long? = null,
    val isConfirmedAwake: Int = 0
)

@Immutable
@Entity(tableName = "step_records")
data class StepRecordEntity(
    @PrimaryKey val dateEpochDay: Long,
    val stepsToday: Long,
    val lastHardwareCounter: Long,
    val hardwareOffset: Long,
    val updatedAtEpochMs: Long
)
