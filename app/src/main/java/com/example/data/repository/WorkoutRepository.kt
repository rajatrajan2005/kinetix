package com.example.data.repository

import com.example.data.local.dao.ExerciseDao
import com.example.data.local.dao.SplitDao
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.SplitExerciseEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutSetEntity
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.OverloadSuggestion
import com.example.domain.model.SplitPreset
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val splitDao: SplitDao,
    private val workoutDao: WorkoutDao
) {
    fun observeSplitDays(): Flow<List<SplitDayEntity>> = splitDao.observeSplitDays()

    fun observeAllSplitExercises(): Flow<List<SplitExerciseEntity>> =
        splitDao.observeAllSplitExercises()

    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutDao.observeActiveSession()

    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> =
        workoutDao.observeCompletedSessions()

    fun observeRecentHistorySets(): Flow<List<WorkoutSetEntity>> =
        workoutDao.observeRecentHistorySets()

    fun observeSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>> =
        workoutDao.observeSetsForSession(sessionId)

    fun observeWeeklyVolume(): Flow<List<MuscleWeeklyVolume>> {
        val sevenDaysAgoMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return workoutDao.observeSetsSince(sevenDaysAgoMs).map { sets ->
            TrainingTuningCalculator.analyzeWeeklyVolume(sets)
        }
    }

    fun observeMuscleGroups(): Flow<List<String>> = exerciseDao.observeMuscleGroups()

    fun observeHeadsForMuscle(muscleGroup: String): Flow<List<String>> =
        exerciseDao.observeHeadsForMuscle(muscleGroup)

    fun observeFilteredExercises(
        muscleGroup: String,
        muscleHead: String,
        query: String
    ): Flow<List<ExerciseEntity>> =
        exerciseDao.observeFiltered(muscleGroup, muscleHead, query)

    suspend fun startSession(day: SplitDayEntity, goal: TrainingGoal): Long {
        val existing = workoutDao.getActiveSessionOnce()
        if (existing != null) return existing.id
        return workoutDao.insertSession(
            WorkoutSessionEntity(
                splitDayId = day.id,
                splitDayName = day.name,
                startedAtEpochMs = System.currentTimeMillis(),
                isActive = 1,
                trainingGoalSnapshot = goal.name
            )
        )
    }

    suspend fun finishSession(sessionId: Long) {
        workoutDao.finishSession(sessionId, System.currentTimeMillis())
    }

    suspend fun discardSession(sessionId: Long) {
        workoutDao.deleteSession(sessionId)
    }

    suspend fun getPreviousSessionData(
        exerciseId: String,
        goal: TrainingGoal,
        targetSetsOverride: Int = 0,
        repMaxOverride: Int = 0
    ): Pair<List<WorkoutSetEntity>, OverloadSuggestion?> {
        val prevSets = workoutDao.getLastSessionSetsForExercise(exerciseId)
        val rx = TrainingTuningCalculator.prescriptionFor(goal)
        val effectiveSets = if (targetSetsOverride > 0) targetSetsOverride else rx.sets
        val effectiveRepMax = if (repMaxOverride > 0) repMaxOverride else rx.repMax
        val suggestion = TrainingTuningCalculator.evaluateDoubleProgression(
            previousSessionSets = prevSets,
            targetSets = effectiveSets,
            repMax = effectiveRepMax
        )
        return prevSets to suggestion
    }

    suspend fun logSet(
        sessionId: Long,
        exerciseId: String,
        exerciseName: String,
        muscleGroup: String,
        muscleHead: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        isWarmup: Boolean = false,
        isAdHoc: Boolean = false
    ) {
        workoutDao.insertSet(
            WorkoutSetEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                muscleGroup = TrainingTuningCalculator.normalizeMuscleGroup(muscleGroup),
                muscleHead = muscleHead,
                setNumber = setNumber,
                weightKg = weightKg.coerceAtLeast(0.0),
                reps = reps.coerceAtLeast(0),
                isHardSet = !isWarmup && reps > 0,
                isWarmup = isWarmup,
                completedAtEpochMs = System.currentTimeMillis(),
                isAdHoc = isAdHoc
            )
        )
    }

    suspend fun deleteSet(setId: Long) {
        workoutDao.deleteSet(setId)
    }

    suspend fun addExerciseToSplitDay(splitDayId: Long, exercise: ExerciseEntity) {
        val nextOrder = splitDao.nextSortOrder(splitDayId)
        splitDao.insertSplitExercise(
            SplitExerciseEntity(
                splitDayId = splitDayId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                muscleGroup = exercise.muscleGroup,
                muscleHead = exercise.muscleHead,
                sortOrder = nextOrder
            )
        )
    }

    suspend fun removeExerciseFromSplit(splitExerciseId: Long) {
        splitDao.deleteSplitExercise(splitExerciseId)
    }

    suspend fun addSplitDay(name: String, targetMusclesCsv: String) {
        val existing = splitDao.getSplitDaysOnce()
        splitDao.insertDay(
            SplitDayEntity(
                name = name.trim(),
                dayOrder = existing.size,
                targetMusclesCsv = targetMusclesCsv.trim()
            )
        )
    }

    suspend fun updateSplitDay(dayId: Long, name: String, targetMusclesCsv: String) {
        val existing = splitDao.getSplitDaysOnce().firstOrNull { it.id == dayId } ?: return
        splitDao.updateDay(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                targetMusclesCsv = targetMusclesCsv.trim()
            )
        )
    }

    suspend fun moveSplitExercise(splitDayId: Long, splitExerciseId: Long, moveUp: Boolean) {
        val items = splitDao.getExercisesForDay(splitDayId).sortedBy { it.sortOrder }.toMutableList()
        val index = items.indexOfFirst { it.id == splitExerciseId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in items.indices) return
        val swapped = items.removeAt(index)
        items.add(targetIndex, swapped)
        val reindexed = items.mapIndexed { idx, entity -> entity.copy(sortOrder = idx) }
        splitDao.updateSplitExercises(reindexed)
    }

    suspend fun reorderSplitExercises(splitDayId: Long, orderedExerciseIds: List<Long>) {
        val items = splitDao.getExercisesForDay(splitDayId)
        val byId = items.associateBy { it.id }
        val reordered = orderedExerciseIds.mapNotNull { byId[it] }
        val remaining = items.filter { it.id !in orderedExerciseIds.toSet() }
        val finalOrder = (reordered + remaining).mapIndexed { idx, entity ->
            entity.copy(sortOrder = idx)
        }
        splitDao.updateSplitExercises(finalOrder)
    }

    suspend fun deleteSplitDay(dayId: Long) {
        splitDao.deleteDay(dayId)
    }

    suspend fun addRestDay() {
        addSplitDay(
            name = "Rest day",
            targetMusclesCsv = "Light walking, Mobility and stretching, Recovery and proper sleep"
        )
    }

    suspend fun getExerciseById(id: String): ExerciseEntity? = exerciseDao.getById(id)

    /**
     * Parses a user-pasted split block such as:
     *   Monday: Chest + Triceps: Flat Bench Press (3x8-10), Incline Dumbbell Press (3x10-12), ...
     */
    suspend fun importSplitFromText(rawText: String): Int {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "[PASTE YOUR SPLIT HERE]" }
        if (lines.isEmpty()) return 0

        val repPattern = Regex("""(?:\(|-|\s)(\d+)\s*[x×]\s*(\d+)(?:\s*[-–]\s*(\d+))?\)?$""", RegexOption.IGNORE_CASE)
        val parsed = mutableListOf<Pair<SplitDayEntity, List<SplitExerciseEntity>>>()
        lines.forEachIndexed { dayIndex, line ->
            val colonIdx = line.indexOf(':')
            val hasColon = colonIdx != -1
            val dayName = if (hasColon) line.substring(0, colonIdx).trim().ifBlank { "Day ${dayIndex + 1}" } else line
            val rhs = if (hasColon) line.substring(colonIdx + 1).trim() else ""
            val isRestLine = dayName.contains("rest", ignoreCase = true) &&
                (rhs.isBlank() || rhs.equals("Rest", ignoreCase = true) || rhs.contains("recovery", ignoreCase = true) || rhs.contains("walking", ignoreCase = true))

            val exTokens = if (isRestLine) {
                emptyList()
            } else {
                rhs.split(",", ";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }

            val resolvedExercises = exTokens.mapIndexed { idx, rawEx ->
                val repMatch = repPattern.find(rawEx)
                val cleanName = if (repMatch != null) rawEx.removeRange(repMatch.range).trim() else rawEx
                val parsedSets = repMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val parsedMin = repMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                val parsedMax = repMatch?.groupValues?.getOrNull(3)?.toIntOrNull() ?: parsedMin

                val resolved = resolveExerciseByNameOrCustom(cleanName)
                val isCompound = resolved.mechanic.equals("Compound", ignoreCase = true)
                val restSec = if (parsedSets > 0) {
                    if (isCompound) COMPOUND_REST_SECONDS else ISOLATION_REST_SECONDS
                } else {
                    0
                }
                SplitExerciseEntity(
                    splitDayId = 0,
                    exerciseId = resolved.id,
                    exerciseName = cleanName,
                    muscleGroup = resolved.muscleGroup,
                    muscleHead = resolved.muscleHead,
                    sortOrder = idx,
                    targetSets = parsedSets,
                    repMin = parsedMin,
                    repMax = parsedMax,
                    restSeconds = restSec
                )
            }

            val muscles = if (isRestLine) {
                rhs.ifBlank { "Light walking, Mobility and stretching, Recovery and proper sleep" }
            } else {
                resolvedExercises.map { it.muscleGroup }.distinct().joinToString(", ").ifBlank { "Custom" }
            }
            parsed.add(
                SplitDayEntity(
                    name = dayName,
                    dayOrder = dayIndex,
                    targetMusclesCsv = muscles
                ) to resolvedExercises
            )
        }

        if (parsed.isNotEmpty()) {
            splitDao.replaceEntireSplit(parsed)
        }
        return parsed.size
    }

    private suspend fun resolveExerciseByNameOrCustom(exName: String): ExerciseEntity {
        val lookupQuery = when {
            exName.contains("chest-supported", ignoreCase = true) -> "Chest-Supported"
            else -> exName
        }
        val match = exerciseDao.findFirstByName(lookupQuery)
        if (match != null) {
            val isChestSupported = exName.contains("chest-supported", ignoreCase = true)
            return if (isChestSupported) {
                match.copy(muscleGroup = "Back", muscleHead = "Mid-back")
            } else {
                match
            }
        }
        val (inferredGroup, inferredHead, inferredMechanic) = inferMuscleAndMechanic(exName)
        val customId = "custom_${exName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
        val created = ExerciseEntity(
            id = customId,
            name = exName,
            muscleGroup = inferredGroup,
            muscleHead = inferredHead,
            secondaryMuscles = "",
            equipment = "Custom",
            level = "Intermediate",
            mechanic = inferredMechanic,
            instructions = "Control the eccentric and focus on progressive overload with clean form.",
            isCustom = 1
        )
        exerciseDao.insertAll(listOf(created))
        return created
    }

    private fun inferMuscleAndMechanic(name: String): Triple<String, String, String> {
        val n = name.lowercase()
        return when {
            "chest-supported" in n -> Triple("Back", "Mid-back", "Compound")
            "pec deck" in n || "fly" in n -> Triple("Chest", "Inner & Mid Chest", "Isolation")
            "bench press" in n || "chest press" in n -> Triple("Chest", if ("incline" in n) "Upper Chest" else "Mid Chest", "Compound")
            "tricep" in n || "pushdown" in n || "skull crusher" in n -> Triple("Triceps", if ("overhead" in n || "skull" in n) "Long Head" else "Lateral Head", "Isolation")
            "pulldown" in n && "straight" in n -> Triple("Back", "Lower Lats", "Isolation")
            "pulldown" in n || "pull-up" in n || "pullup" in n || "row" in n -> Triple("Back", if ("row" in n) "Mid-back" else "Lats", "Compound")
            "reverse curl" in n -> Triple("Biceps", "Brachioradialis & Forearms", "Isolation")
            "hammer curl" in n -> Triple("Biceps", "Brachialis & Forearms", "Isolation")
            "curl" in n && "leg" !in n -> Triple("Biceps", "Biceps", "Isolation")
            "lateral raise" in n || "reverse pec" in n || "face pull" in n -> Triple("Shoulders", if ("reverse" in n || "face" in n) "Rear Delt" else "Lateral Delt", "Isolation")
            "shoulder press" in n || "overhead press" in n -> Triple("Shoulders", "Front & Side Delts", "Compound")
            "crunch" in n || "knee raise" in n || "plank" in n -> Triple("Abs", "Core & Abs", "Isolation")
            "squat" in n || "leg press" in n || "leg extension" in n -> Triple("Quads", "Quadriceps", if ("extension" in n) "Isolation" else "Compound")
            "deadlift" in n -> Triple("Hamstrings", "Hamstrings & Glutes", "Compound")
            "leg curl" in n -> Triple("Hamstrings", "Hamstrings", "Isolation")
            "calf" in n -> Triple("Calves", "Gastrocnemius", "Isolation")
            else -> Triple("Full Body", "General", "Compound")
        }
    }

    /**
     * Repairs mislabeled Chest-Supported Row muscle tags and auto-upgrades untouched
     * legacy starter splits to the canonical 6-Day Double Muscle Split.
     */
    suspend fun repairSplitAndExerciseTags() {
        exerciseDao.repairChestSupportedRowTags()
        splitDao.repairChestSupportedRowTags()
        workoutDao.repairChestSupportedRowTags()

        val days = splitDao.getSplitDaysOnce()
        if (days.isEmpty()) {
            applySplitPreset(SplitPreset.DOUBLE_MUSCLE)
            return
        }
        val hasSessions = workoutDao.countAllSessions() > 0
        val looksLikePreviousFourDayStarter =
            days.size == 4 &&
                days[0].name == "Chest + Triceps" &&
                days[1].name == "Back + Biceps" &&
                days[2].name == "Legs" &&
                days[3].name == "Shoulders"
        val looksLikeUntouchedOldStarter = !hasSessions && (
            days.any { it.name.contains("(Repeat)", ignoreCase = true) } ||
                looksLikePreviousFourDayStarter ||
                (days.size == 3 &&
                    days[0].name.startsWith("Push (", ignoreCase = true) &&
                    days[1].name.startsWith("Pull (", ignoreCase = true) &&
                    days[2].name.startsWith("Legs (", ignoreCase = true))
            )
        if (looksLikeUntouchedOldStarter) {
            applySplitPreset(SplitPreset.DOUBLE_MUSCLE)
        }
    }

    /**
     * Seeds the canonical 6-Day Double Muscle Split if the user has zero split days.
     */
    suspend fun ensureStarterSplitIfEmpty() {
        if (splitDao.getSplitDaysOnce().isNotEmpty()) return
        applySplitPreset(SplitPreset.DOUBLE_MUSCLE)
    }

    suspend fun applySplitPreset(preset: SplitPreset) {
        when (preset) {
            SplitPreset.DOUBLE_MUSCLE -> applyDoubleMusclePreset()
            SplitPreset.PPL -> {
                val text = """
                    Push (Chest / Delts / Triceps): Barbell Bench Press, Incline Dumbbell Press, Dumbbell Lateral Raise, Overhead Cable Triceps Extension
                    Pull (Back / Rear Delts / Biceps): Pull-Up, Chest-Supported Row, Rope Face Pull, Incline Dumbbell Curl
                    Legs (Quads / Hamstrings / Calves): Barbell Back Squat, Romanian Deadlift, Leg Extension Machine, Standing Calf Raise
                """.trimIndent()
                importSplitFromText(text)
            }
        }
    }

    private suspend fun applyDoubleMusclePreset() {
        val daysWithExercises = DOUBLE_MUSCLE_SCHEDULE.mapIndexed { dayIndex, daySpec ->
            val resolvedExercises = daySpec.exercises.mapIndexed { exIndex, exSpec ->
                val resolved = resolveDoubleMuscleExercise(exSpec)
                SplitExerciseEntity(
                    splitDayId = 0,
                    exerciseId = resolved.id,
                    exerciseName = exSpec.name,
                    muscleGroup = exSpec.muscleGroup,
                    muscleHead = exSpec.muscleHead,
                    sortOrder = exIndex,
                    targetSets = exSpec.sets,
                    repMin = exSpec.repMin,
                    repMax = exSpec.repMax,
                    restSeconds = exSpec.restSeconds
                )
            }
            SplitDayEntity(
                name = daySpec.dayTitle,
                dayOrder = dayIndex,
                targetMusclesCsv = daySpec.targetMusclesCsv
            ) to resolvedExercises
        }
        splitDao.replaceEntireSplit(daysWithExercises)
    }

    private suspend fun resolveDoubleMuscleExercise(spec: DoubleMuscleExerciseSpec): ExerciseEntity {
        for (alias in spec.lookupAliases) {
            val match = exerciseDao.findFirstByName(alias)
            if (match != null && match.muscleGroup.equals(spec.muscleGroup, ignoreCase = true)) {
                return match
            }
        }
        val canonicalId = "dm_${spec.slug}"
        val existing = exerciseDao.getById(canonicalId)
        if (existing != null) return existing
        val created = ExerciseEntity(
            id = canonicalId,
            name = spec.name,
            muscleGroup = spec.muscleGroup,
            muscleHead = spec.muscleHead,
            secondaryMuscles = spec.secondaryMuscles,
            equipment = spec.equipment,
            level = "Intermediate",
            mechanic = spec.mechanic,
            instructions = spec.instructions,
            isCustom = 0
        )
        exerciseDao.insertAll(listOf(created))
        return created
    }

    suspend fun ensureSportExercisesSeeded() {
        exerciseDao.insertAll(SPORT_EXERCISES)
        exerciseDao.repairChestSupportedRowTags()
    }

    fun observeAllExercises(): Flow<List<ExerciseEntity>> =
        exerciseDao.observeFiltered(muscleGroup = "", muscleHead = "", query = "")

    fun recommendedForSport(
        sport: SportDiscipline,
        allExercises: List<ExerciseEntity>
    ): List<ExerciseEntity> {
        if (sport == SportDiscipline.NONE) return emptyList()
        val ids = when (sport) {
            SportDiscipline.NONE -> emptyList()
            SportDiscipline.FOOTBALL -> listOf("sport_nordic_curl", "sport_copenhagen_plank", "sport_sled_sprint", "sport_box_jump", "sport_bulgarian_split")
            SportDiscipline.BASKETBALL -> listOf("sport_box_jump", "sport_bulgarian_split", "sport_lateral_bound", "sport_nordic_curl", "sport_tibialis_raise")
            SportDiscipline.RUNNING -> listOf("sport_single_leg_calf", "sport_tibialis_raise", "sport_bulgarian_split", "sport_nordic_curl", "sport_copenhagen_plank")
            SportDiscipline.CRICKET -> listOf("sport_medball_slam", "sport_external_rotation", "sport_lateral_bound", "sport_bulgarian_split", "sport_scapular_pushup")
            SportDiscipline.COMBAT -> listOf("sport_medball_slam", "sport_sled_sprint", "sport_neck_bridge", "sport_towel_pullup", "sport_box_jump")
            SportDiscipline.SWIMMING -> listOf("sport_scapular_pushup", "sport_external_rotation", "sport_straight_arm_pulldown", "sport_hollow_hold", "sport_towel_pullup")
            SportDiscipline.CYCLING -> listOf("sport_bulgarian_split", "sport_nordic_curl", "sport_single_leg_calf", "sport_hollow_hold", "sport_copenhagen_plank")
        }
        val byId = allExercises.associateBy { it.id }
        val direct = ids.mapNotNull { byId[it] }
        return direct.ifEmpty { SPORT_EXERCISES.filter { it.id in ids } }
    }

    companion object {
        const val COMPOUND_REST_SECONDS = 150
        const val ISOLATION_REST_SECONDS = 75

        private data class DoubleMuscleExerciseSpec(
            val slug: String,
            val name: String,
            val sets: Int,
            val repMin: Int,
            val repMax: Int,
            val muscleGroup: String,
            val muscleHead: String,
            val secondaryMuscles: String,
            val equipment: String,
            val mechanic: String,
            val instructions: String,
            val lookupAliases: List<String> = emptyList()
        ) {
            val restSeconds: Int
                get() = if (mechanic.equals("Compound", ignoreCase = true)) {
                    COMPOUND_REST_SECONDS
                } else {
                    ISOLATION_REST_SECONDS
                }
        }

        private data class DoubleMuscleDaySpec(
            val dayTitle: String,
            val targetMusclesCsv: String,
            val exercises: List<DoubleMuscleExerciseSpec>
        )

        private val DOUBLE_MUSCLE_SCHEDULE = listOf(
            DoubleMuscleDaySpec(
                dayTitle = "Monday: Chest + Triceps",
                targetMusclesCsv = "Chest, Triceps",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "flat_bench_press",
                        name = "Flat Bench Press",
                        sets = 3, repMin = 8, repMax = 10,
                        muscleGroup = "Chest", muscleHead = "Mid Chest",
                        secondaryMuscles = "Anterior Delts, Triceps",
                        equipment = "Barbell", mechanic = "Compound",
                        instructions = "Set shoulder blades down and back, lower bar smoothly to mid-chest, and drive explosively with 1–3 reps in reserve.",
                        lookupAliases = listOf("Barbell Bench Press", "Flat Bench Press")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "incline_dumbbell_press",
                        name = "Incline Dumbbell Press",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Chest", muscleHead = "Upper Chest",
                        secondaryMuscles = "Anterior Delts, Triceps",
                        equipment = "Dumbbell", mechanic = "Compound",
                        instructions = "Use a 30-degree bench angle, lower dumbbells deep with elbows tucked ~45 degrees, and press cleanly.",
                        lookupAliases = listOf("Incline Dumbbell Press")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "pec_deck",
                        name = "Pec Deck",
                        sets = 3, repMin = 12, repMax = 15,
                        muscleGroup = "Chest", muscleHead = "Inner & Mid Chest",
                        secondaryMuscles = "Anterior Delts",
                        equipment = "Machine", mechanic = "Isolation",
                        instructions = "Keep shoulders pinned to the pad and sweep elbows inward through a full controlled chest contraction.",
                        lookupAliases = listOf("Pec Deck", "Chest Fly Machine")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "triceps_rope_pushdown",
                        name = "Triceps Rope Pushdown",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Triceps", muscleHead = "Lateral Head",
                        secondaryMuscles = "Medial Head",
                        equipment = "Cable", mechanic = "Isolation",
                        instructions = "Pin elbows at your sides, extend fully while spreading the rope ends apart at the bottom.",
                        lookupAliases = listOf("Rope Triceps Pushdown", "Triceps Rope Pushdown", "Triceps Pushdown")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "overhead_cable_triceps_extension",
                        name = "Overhead Cable Triceps Extension",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Triceps", muscleHead = "Long Head",
                        secondaryMuscles = "Lateral Head",
                        equipment = "Cable", mechanic = "Isolation",
                        instructions = "Stretch the triceps long head deeply behind your head and extend cleanly without flaring elbows.",
                        lookupAliases = listOf("Overhead Cable Triceps Extension", "Overhead Triceps")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Tuesday: Back + Biceps",
                targetMusclesCsv = "Back, Biceps",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "lat_pulldown",
                        name = "Lat Pulldown",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Back", muscleHead = "Lats",
                        secondaryMuscles = "Biceps, Mid-back",
                        equipment = "Cable", mechanic = "Compound",
                        instructions = "Depress shoulder blades first and pull the bar to your upper chest with controlled tempo.",
                        lookupAliases = listOf("Lat Pulldown", "Wide-Grip Lat Pulldown")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "seated_cable_row",
                        name = "Seated Cable Row",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Back", muscleHead = "Mid-back",
                        secondaryMuscles = "Lats, Biceps, Rear Delts",
                        equipment = "Cable", mechanic = "Compound",
                        instructions = "Keep torso tall, allow full scapular stretch forward, and drive elbows past your ribs.",
                        lookupAliases = listOf("Seated Cable Row", "Cable Row")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "chest_supported_row",
                        name = "Chest-Supported Row",
                        sets = 3, repMin = 10, repMax = 10,
                        muscleGroup = "Back", muscleHead = "Mid-back",
                        secondaryMuscles = "Lats, Rear Delts, Biceps",
                        equipment = "Machine", mechanic = "Compound",
                        instructions = "Anchor chest firmly to the pad to remove momentum and squeeze mid-back through full range.",
                        lookupAliases = listOf("Chest-Supported Row", "Chest-Supported")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "barbell_bicep_curl",
                        name = "Barbell Bicep Curl",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Biceps", muscleHead = "Short & Long Head",
                        secondaryMuscles = "Brachialis, Forearms",
                        equipment = "Barbell", mechanic = "Isolation",
                        instructions = "Keep elbows anchored near your ribs and curl smoothly without swinging your lower back.",
                        lookupAliases = listOf("Barbell Bicep Curl", "Barbell Curl")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "hammer_curl",
                        name = "Hammer Curl",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Biceps", muscleHead = "Brachialis & Forearms",
                        secondaryMuscles = "Brachioradialis",
                        equipment = "Dumbbell", mechanic = "Isolation",
                        instructions = "Maintain a neutral palms-facing grip to build upper-arm thickness and forearm density.",
                        lookupAliases = listOf("Dumbbell Hammer Curl", "Hammer Curl")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Wednesday: Shoulders + Abs",
                targetMusclesCsv = "Shoulders, Abs",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "shoulder_press",
                        name = "Shoulder Press",
                        sets = 3, repMin = 8, repMax = 10,
                        muscleGroup = "Shoulders", muscleHead = "Front & Side Delts",
                        secondaryMuscles = "Triceps, Upper Traps",
                        equipment = "Dumbbell / Barbell", mechanic = "Compound",
                        instructions = "Brace core and glutes, press overhead in a straight vertical path without excessive lumbar arch.",
                        lookupAliases = listOf("Barbell Overhead Press", "Dumbbell Shoulder Press", "Shoulder Press")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "dumbbell_lateral_raises",
                        name = "Dumbbell Lateral Raises",
                        sets = 3, repMin = 12, repMax = 15,
                        muscleGroup = "Shoulders", muscleHead = "Lateral Delt",
                        secondaryMuscles = "Upper Traps",
                        equipment = "Dumbbell", mechanic = "Isolation",
                        instructions = "Lead with elbows slightly forward in the scapular plane and lower slowly with zero swinging.",
                        lookupAliases = listOf("Dumbbell Lateral Raise", "Lateral Raise")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "reverse_pec_deck",
                        name = "Reverse Pec Deck",
                        sets = 3, repMin = 12, repMax = 15,
                        muscleGroup = "Shoulders", muscleHead = "Rear Delt",
                        secondaryMuscles = "Mid-back, Rhomboids",
                        equipment = "Machine", mechanic = "Isolation",
                        instructions = "Sweep arms outward with soft elbows, targeting the posterior delts without shrugging.",
                        lookupAliases = listOf("Reverse Pec Deck", "Rear Delt Fly Machine")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "cable_crunches",
                        name = "Cable Crunches",
                        sets = 3, repMin = 15, repMax = 15,
                        muscleGroup = "Abs", muscleHead = "Upper & Mid Abs",
                        secondaryMuscles = "Obliques",
                        equipment = "Cable", mechanic = "Isolation",
                        instructions = "Keep hips stationary and flex the thoracic spine downward toward your pelvis using your abs.",
                        lookupAliases = listOf("Cable Crunch", "Cable Crunches")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "hanging_knee_raises",
                        name = "Hanging Knee Raises",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Abs", muscleHead = "Lower Abs",
                        secondaryMuscles = "Hip Flexors, Forearms",
                        equipment = "Pull-Up Bar", mechanic = "Isolation",
                        instructions = "Curl your pelvis upward toward your ribcage at the top and lower with control to eliminate swing.",
                        lookupAliases = listOf("Hanging Knee Raise", "Hanging Knee Raises")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Thursday: Chest + Triceps",
                targetMusclesCsv = "Chest, Triceps",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "incline_bench_press",
                        name = "Incline Bench Press",
                        sets = 3, repMin = 8, repMax = 10,
                        muscleGroup = "Chest", muscleHead = "Upper Chest",
                        secondaryMuscles = "Anterior Delts, Triceps",
                        equipment = "Barbell", mechanic = "Compound",
                        instructions = "Lower bar with control to the upper sternum and press smoothly along a steady bar path.",
                        lookupAliases = listOf("Incline Barbell Bench Press", "Incline Bench Press")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "machine_chest_press",
                        name = "Machine Chest Press",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Chest", muscleHead = "Mid Chest",
                        secondaryMuscles = "Triceps, Anterior Delts",
                        equipment = "Machine", mechanic = "Compound",
                        instructions = "Align handles with mid-chest and push safely close to technical failure without losing scapular position.",
                        lookupAliases = listOf("Machine Chest Press", "Chest Press Machine")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "pec_deck_fly",
                        name = "Pec Deck Fly",
                        sets = 3, repMin = 12, repMax = 15,
                        muscleGroup = "Chest", muscleHead = "Inner & Mid Chest",
                        secondaryMuscles = "Anterior Delts",
                        equipment = "Machine", mechanic = "Isolation",
                        instructions = "Focus on a deep horizontal chest stretch and controlled 1-second squeeze at peak contraction.",
                        lookupAliases = listOf("Pec Deck Fly")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "skull_crushers",
                        name = "Skull Crushers",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Triceps", muscleHead = "Long & Medial Head",
                        secondaryMuscles = "Lateral Head",
                        equipment = "EZ Bar / Barbell", mechanic = "Isolation",
                        instructions = "Angle upper arms slightly backward behind vertical for constant triceps tension on every rep.",
                        lookupAliases = listOf("Skull Crusher", "Skull Crushers", "Lying Triceps Extension")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "single_arm_cable_pushdown",
                        name = "Single-Arm Cable Pushdown",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Triceps", muscleHead = "Lateral Head",
                        secondaryMuscles = "Medial Head",
                        equipment = "Cable", mechanic = "Isolation",
                        instructions = "Work one arm at a time with full elbow lockout to even out triceps strength and development.",
                        lookupAliases = listOf("Single-Arm Cable Pushdown", "One-Arm Cable Pushdown")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Friday: Back + Biceps",
                targetMusclesCsv = "Back, Biceps",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "pull_ups",
                        name = "Pull-ups",
                        sets = 3, repMin = 6, repMax = 10,
                        muscleGroup = "Back", muscleHead = "Lats",
                        secondaryMuscles = "Biceps, Mid-back, Core",
                        equipment = "Pull-Up Bar", mechanic = "Compound",
                        instructions = "Start from a full dead-hang shoulder stretch, drive elbows down toward your hips, and clear the bar.",
                        lookupAliases = listOf("Pull-Up", "Pull-ups")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "t_bar_row",
                        name = "T-Bar Row",
                        sets = 3, repMin = 8, repMax = 12,
                        muscleGroup = "Back", muscleHead = "Mid-back",
                        secondaryMuscles = "Lats, Rear Delts, Biceps",
                        equipment = "T-Bar / Barbell", mechanic = "Compound",
                        instructions = "Hinge firmly with a flat back and pull through the elbows for dense upper- and mid-back thickness.",
                        lookupAliases = listOf("T-Bar Row")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "straight_arm_pulldown",
                        name = "Straight-Arm Pulldown",
                        sets = 3, repMin = 12, repMax = 15,
                        muscleGroup = "Back", muscleHead = "Lower Lats",
                        secondaryMuscles = "Teres Major, Triceps Long Head",
                        equipment = "Cable", mechanic = "Isolation",
                        instructions = "Keep elbows softly locked and sweep the bar down to your thighs using pure lat contraction.",
                        lookupAliases = listOf("Straight-Arm Lat Pulldown", "Straight-Arm Pulldown")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "incline_dumbbell_curl",
                        name = "Incline Dumbbell Curl",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Biceps", muscleHead = "Long Head",
                        secondaryMuscles = "Short Head, Forearms",
                        equipment = "Dumbbell", mechanic = "Isolation",
                        instructions = "Let arms hang behind your torso on a 45-degree incline bench for maximum long-head stretch.",
                        lookupAliases = listOf("Incline Dumbbell Curl")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "reverse_curl",
                        name = "Reverse Curl",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Biceps", muscleHead = "Brachioradialis & Forearms",
                        secondaryMuscles = "Brachialis",
                        equipment = "Barbell / EZ Bar", mechanic = "Isolation",
                        instructions = "Use an overhand pronated grip to build outer-arm brachialis and forearm thickness.",
                        lookupAliases = listOf("Reverse Barbell Curl", "Reverse Curl")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Saturday: Legs",
                targetMusclesCsv = "Quads, Hamstrings, Calves",
                exercises = listOf(
                    DoubleMuscleExerciseSpec(
                        slug = "barbell_squats",
                        name = "Barbell Squats",
                        sets = 3, repMin = 8, repMax = 10,
                        muscleGroup = "Quads", muscleHead = "Quads & Glutes",
                        secondaryMuscles = "Adductors, Core, Spinal Erectors",
                        equipment = "Barbell", mechanic = "Compound",
                        instructions = "Brace 360 degrees, descend smoothly to parallel or deeper, and drive evenly through mid-foot.",
                        lookupAliases = listOf("Barbell Back Squat", "Barbell Squat")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "leg_press",
                        name = "Leg Press",
                        sets = 3, repMin = 10, repMax = 12,
                        muscleGroup = "Quads", muscleHead = "Quads & Glutes",
                        secondaryMuscles = "Adductors, Hamstrings",
                        equipment = "Machine", mechanic = "Compound",
                        instructions = "Keep lower back and pelvis glued to the seat pad and descend through full knee flexion.",
                        lookupAliases = listOf("Leg Press", "45-Degree Leg Press")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "romanian_deadlift",
                        name = "Romanian Deadlift",
                        sets = 3, repMin = 10, repMax = 10,
                        muscleGroup = "Hamstrings", muscleHead = "Hamstrings & Glutes",
                        secondaryMuscles = "Spinal Erectors, Forearms",
                        equipment = "Barbell", mechanic = "Compound",
                        instructions = "Push hips straight back while keeping the barbell shaved close to legs until hamstrings are fully loaded.",
                        lookupAliases = listOf("Romanian Deadlift")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "leg_curls",
                        name = "Leg Curls",
                        sets = 3, repMin = 12, repMax = 12,
                        muscleGroup = "Hamstrings", muscleHead = "Hamstrings",
                        secondaryMuscles = "Calves",
                        equipment = "Machine", mechanic = "Isolation",
                        instructions = "Pin hips to the pad, curl heels smoothly, and control a 2–3 second eccentric.",
                        lookupAliases = listOf("Seated Leg Curl", "Lying Leg Curl", "Leg Curl")
                    ),
                    DoubleMuscleExerciseSpec(
                        slug = "standing_calf_raises",
                        name = "Standing Calf Raises",
                        sets = 3, repMin = 15, repMax = 15,
                        muscleGroup = "Calves", muscleHead = "Gastrocnemius",
                        secondaryMuscles = "Soleus",
                        equipment = "Machine", mechanic = "Isolation",
                        instructions = "Pause 1 second in the deep bottom stretch on every rep before driving high onto the big toe ball.",
                        lookupAliases = listOf("Standing Calf Raise", "Standing Calf Raises")
                    )
                )
            ),
            DoubleMuscleDaySpec(
                dayTitle = "Sunday: Rest",
                targetMusclesCsv = "Light walking, Mobility and stretching, Recovery and proper sleep",
                exercises = emptyList()
            )
        )

        private val SPORT_EXERCISES = listOf(
            ExerciseEntity(
                id = "sport_nordic_curl",
                name = "Nordic Hamstring Curl",
                muscleGroup = "Hamstrings",
                muscleHead = "Proximal & Distal Hamstrings",
                secondaryMuscles = "Glutes, Calves",
                equipment = "Bodyweight",
                level = "Advanced",
                mechanic = "Isolation",
                instructions = "Lower slowly under eccentric tension to bullet-proof sprint hamstrings."
            ),
            ExerciseEntity(
                id = "sport_copenhagen_plank",
                name = "Copenhagen Adductor Plank",
                muscleGroup = "Glutes",
                muscleHead = "Hip Adductors & Core",
                secondaryMuscles = "Abs, Obliques",
                equipment = "Bench",
                level = "Intermediate",
                mechanic = "Isolation",
                instructions = "Stabilize pelvis with top leg on bench to prevent groin strains."
            ),
            ExerciseEntity(
                id = "sport_sled_sprint",
                name = "Resisted Sled / Drive Sprint",
                muscleGroup = "Quads",
                muscleHead = "Acceleration Drive",
                secondaryMuscles = "Glutes, Calves",
                equipment = "Sled / Track",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Drive knees forward at 45-degree lean for explosive first-step acceleration."
            ),
            ExerciseEntity(
                id = "sport_box_jump",
                name = "Plyometric Box Jump",
                muscleGroup = "Quads",
                muscleHead = "Fast-Twitch Power",
                secondaryMuscles = "Glutes, Calves",
                equipment = "Plyo Box",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Explode vertically with arm swing and land softly in athletic quarter-squat."
            ),
            ExerciseEntity(
                id = "sport_bulgarian_split",
                name = "Bulgarian Split Squat",
                muscleGroup = "Quads",
                muscleHead = "Unilateral Balance",
                secondaryMuscles = "Glutes, Hamstrings",
                equipment = "Dumbbell",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Build single-leg strength, knee tracking, and field deceleration stability."
            ),
            ExerciseEntity(
                id = "sport_lateral_bound",
                name = "Lateral Skater Bound",
                muscleGroup = "Glutes",
                muscleHead = "Gluteus Medius / Lateral",
                secondaryMuscles = "Quads, Calves",
                equipment = "Bodyweight",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Bound side to side and stick each single-leg landing cleanly."
            ),
            ExerciseEntity(
                id = "sport_single_leg_calf",
                name = "Single-Leg Pogos & Calf Raise",
                muscleGroup = "Calves",
                muscleHead = "Achilles & Soleus",
                secondaryMuscles = "Gastrocnemius",
                equipment = "Step / Plate",
                level = "Beginner",
                mechanic = "Isolation",
                instructions = "Develop tendon stiffness and ground-contact elasticity for running."
            ),
            ExerciseEntity(
                id = "sport_tibialis_raise",
                name = "Tibialis Wall Raise",
                muscleGroup = "Calves",
                muscleHead = "Anterior Tibialis",
                secondaryMuscles = "Ankle Stabilizers",
                equipment = "Bodyweight",
                level = "Beginner",
                mechanic = "Isolation",
                instructions = "Pull toes upward against wall lean to protect knees and shins."
            ),
            ExerciseEntity(
                id = "sport_medball_slam",
                name = "Rotational Med Ball Slam",
                muscleGroup = "Core",
                muscleHead = "Rotational Obliques",
                secondaryMuscles = "Shoulders, Lats",
                equipment = "Medicine Ball",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Generate hip-to-shoulder torque and slam explosively for striking or throwing."
            ),
            ExerciseEntity(
                id = "sport_external_rotation",
                name = "Cable External Rotation",
                muscleGroup = "Shoulders",
                muscleHead = "Rotator Cuff / Rear Delt",
                secondaryMuscles = "Scapular Stabilizers",
                equipment = "Cable",
                level = "Beginner",
                mechanic = "Isolation",
                instructions = "Keep elbow pinned at 90 degrees and rotate outward for throwing/overhead longevity."
            ),
            ExerciseEntity(
                id = "sport_scapular_pushup",
                name = "Scapular Serratus Push-Up",
                muscleGroup = "Shoulders",
                muscleHead = "Serratus & Scapula",
                secondaryMuscles = "Chest, Core",
                equipment = "Bodyweight",
                level = "Beginner",
                mechanic = "Isolation",
                instructions = "Protract and retract shoulder blades smoothly with straight elbows."
            ),
            ExerciseEntity(
                id = "sport_straight_arm_pulldown",
                name = "Straight-Arm Lat Pulldown",
                muscleGroup = "Back",
                muscleHead = "Lower Lats",
                secondaryMuscles = "Core, Triceps Long Head",
                equipment = "Cable",
                level = "Beginner",
                mechanic = "Isolation",
                instructions = "Sweep straight arms downward to mimic swim pull and build lat endurance."
            ),
            ExerciseEntity(
                id = "sport_hollow_hold",
                name = "Streamline Hollow Body Hold",
                muscleGroup = "Core",
                muscleHead = "Deep Transverse Abdominis",
                secondaryMuscles = "Hip Flexors",
                equipment = "Bodyweight",
                level = "Intermediate",
                mechanic = "Isolation",
                instructions = "Press lower back into floor and brace arms overhead in sleek streamline."
            ),
            ExerciseEntity(
                id = "sport_towel_pullup",
                name = "Towel / Fat-Grip Pull-Up",
                muscleGroup = "Back",
                muscleHead = "Lats & Crush Grip",
                secondaryMuscles = "Biceps, Forearms",
                equipment = "Pull-Up Bar",
                level = "Advanced",
                mechanic = "Compound",
                instructions = "Build relentless pulling strength and grappling grip endurance."
            ),
            ExerciseEntity(
                id = "sport_neck_bridge",
                name = "Isometric Neck Harness / Hold",
                muscleGroup = "Back",
                muscleHead = "Upper Traps & Neck",
                secondaryMuscles = "Cervical Stabilizers",
                equipment = "Plate / Band",
                level = "Intermediate",
                mechanic = "Isolation",
                instructions = "Controlled multi-angle neck resistance for combat and contact resilience."
            )
        )
    }
}
