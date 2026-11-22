package com.example.domain.model

import androidx.compose.runtime.Immutable

enum class Sex(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class ActivityLevel(
    val label: String,
    val factor: Double,
    val subtitle: String
) {
    SEDENTARY("Sedentary", 1.2, "Desk job, minimal exercise"),
    LIGHT("Light", 1.375, "1–3 light workouts / week"),
    MODERATE("Moderate", 1.55, "3–5 training sessions / week"),
    VERY_ACTIVE("Very Active", 1.725, "6–7 hard sessions / week"),
    ATHLETE("Athlete", 1.9, "Twice-daily or heavy labor")
}

enum class NutritionGoal(
    val label: String,
    val subtitle: String
) {
    CUT("Cut (Fat Loss)", "TDEE minus 15–20% deficit"),
    MAINTAIN("Maintain", "Bodyweight & performance steady"),
    RECOMP("Body Recomp", "TDEE maintenance · lean muscle + fat loss"),
    BULK("Bulk (Lean Gain)", "TDEE plus 10% surplus")
}

enum class TrainingGoal(
    val label: String,
    val subtitle: String,
    val defaultSets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int
) {
    STRENGTH(
        label = "Strength",
        subtitle = "3–5 sets × 3–6 reps · 3 min rest",
        defaultSets = 4,
        repMin = 3,
        repMax = 6,
        restSeconds = 180
    ),
    HYPERTROPHY(
        label = "Hypertrophy",
        subtitle = "3–4 sets × 6–12 reps · 90s rest",
        defaultSets = 3,
        repMin = 6,
        repMax = 12,
        restSeconds = 90
    ),
    BODY_RECOMP(
        label = "Body Recomp",
        subtitle = "3–4 sets × 8–12 reps · 90s rest · high protein",
        defaultSets = 3,
        repMin = 8,
        repMax = 12,
        restSeconds = 90
    ),
    FAT_LOSS(
        label = "Fat Loss",
        subtitle = "Hypertrophy stimulus · high protein & steps",
        defaultSets = 3,
        repMin = 6,
        repMax = 12,
        restSeconds = 90
    ),
    ENDURANCE(
        label = "Endurance",
        subtitle = "2–3 sets × 12–20 reps · 60s rest",
        defaultSets = 3,
        repMin = 12,
        repMax = 20,
        restSeconds = 60
    )
}

enum class SportDiscipline(
    val label: String,
    val subtitle: String
) {
    NONE("Gym Only", "Pure strength, hypertrophy & physique focus"),
    FOOTBALL("Football / Soccer", "Sprint power, hamstring/groin durability & agility"),
    BASKETBALL("Basketball", "Vertical jump, deceleration & lateral reactivity"),
    RUNNING("Running / Track", "Single-leg drive, calf elasticity & hip stability"),
    CRICKET("Cricket / Racquet", "Rotational power, cuff health & lateral speed"),
    COMBAT("Combat / MMA", "Explosive hips, rotational core & grip endurance"),
    SWIMMING("Swimming", "Lat endurance, scapular control & streamline core"),
    CYCLING("Cycling", "Quad stamina, glute-ham balance & lumbar endurance")
}

enum class SplitPreset(
    val label: String,
    val subtitle: String
) {
    DOUBLE_MUSCLE(
        "6-Day Double Muscle (Default)",
        "Mon–Sat training · Chest+Tri · Back+Bi · Shoulders+Abs · Legs · Sunday Rest"
    ),
    PPL("Push · Pull · Legs", "3-day push, pull & legs split")
}

enum class MealSlot(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack")
}

@Immutable
data class MacroTargets(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val targetKcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int
)

@Immutable
data class SetRepPrescription(
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val note: String
)

@Immutable
data class OverloadSuggestion(
    val shouldIncrease: Boolean,
    val suggestedWeightKg: Double,
    val reason: String
)

@Immutable
data class MuscleWeeklyVolume(
    val muscleGroup: String,
    val hardSets: Int,
    val status: VolumeStatus
)

enum class VolumeStatus {
    UNDER,    // < 10 hard sets / 7 days
    OPTIMAL,  // 10..20 hard sets / 7 days
    EXCESS    // > 20 hard sets / 7 days
}

@Immutable
data class WaterDoseSlot(
    val triggerEpochMs: Long,
    val amountMl: Int,
    val isCatchUp: Boolean
)
