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
        goal: TrainingGoal
    ): Pair<List<WorkoutSetEntity>, OverloadSuggestion?> {
        val prevSets = workoutDao.getLastSessionSetsForExercise(exerciseId)
        val rx = TrainingTuningCalculator.prescriptionFor(goal)
        val suggestion = TrainingTuningCalculator.evaluateDoubleProgression(
            previousSessionSets = prevSets,
            targetSets = rx.sets,
            repMax = rx.repMax
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

    suspend fun deleteSplitDay(dayId: Long) {
        splitDao.deleteDay(dayId)
    }

    /**
     * Parses a user-pasted split block such as:
     *   Push: Barbell Bench Press, Incline Dumbbell Press, Lateral Raise, Triceps Pushdown
     *   Pull: Pull-Up, Barbell Row, Face Pull, Incline Dumbbell Curl
     *   Legs: Barbell Back Squat, Romanian Deadlift, Leg Curl, Standing Calf Raise
     */
    suspend fun importSplitFromText(rawText: String): Int {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "[PASTE YOUR SPLIT HERE]" }
        if (lines.isEmpty()) return 0

        val parsed = mutableListOf<Pair<SplitDayEntity, List<SplitExerciseEntity>>>()
        lines.forEachIndexed { dayIndex, line ->
            val parts = line.split(":", limit = 2)
            val dayName = parts.first().trim().ifBlank { "Day ${dayIndex + 1}" }
            val exNames = parts.getOrNull(1)
                ?.split(",", ";")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            val resolvedExercises = exNames.mapIndexed { idx, exName ->
                val match = exerciseDao.findFirstByName(exName)
                if (match != null) {
                    SplitExerciseEntity(
                        splitDayId = 0,
                        exerciseId = match.id,
                        exerciseName = match.name,
                        muscleGroup = match.muscleGroup,
                        muscleHead = match.muscleHead,
                        sortOrder = idx
                    )
                } else {
                    val customId = "custom_${exName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
                    exerciseDao.insertAll(
                        listOf(
                            ExerciseEntity(
                                id = customId,
                                name = exName,
                                muscleGroup = "Full Body",
                                muscleHead = "General",
                                secondaryMuscles = "",
                                equipment = "Custom",
                                level = "Intermediate",
                                mechanic = "Compound",
                                instructions = "Custom split exercise",
                                isCustom = 1
                            )
                        )
                    )
                    SplitExerciseEntity(
                        splitDayId = 0,
                        exerciseId = customId,
                        exerciseName = exName,
                        muscleGroup = "Full Body",
                        muscleHead = "General",
                        sortOrder = idx
                    )
                }
            }

            val muscles = resolvedExercises.map { it.muscleGroup }.distinct().joinToString(", ")
            parsed.add(
                SplitDayEntity(
                    name = dayName,
                    dayOrder = dayIndex,
                    targetMusclesCsv = muscles.ifBlank { "Custom" }
                ) to resolvedExercises
            )
        }

        if (parsed.isNotEmpty()) {
            splitDao.replaceEntireSplit(parsed)
        }
        return parsed.size
    }

    /**
     * Seeds an editable starter PPL split only if user has zero split days.
     */
    suspend fun ensureStarterSplitIfEmpty() {
        if (splitDao.getSplitDaysOnce().isNotEmpty()) return
        applySplitPreset(SplitPreset.PPL)
    }

    suspend fun applySplitPreset(preset: SplitPreset) {
        val text = when (preset) {
            SplitPreset.PPL -> """
                Push (Chest / Delts / Triceps): Barbell Bench Press, Incline Dumbbell Press, Dumbbell Lateral Raise, Overhead Cable Triceps Extension
                Pull (Back / Rear Delts / Biceps): Pull-Up, Chest-Supported Row, Face Pull, Incline Dumbbell Curl
                Legs (Quads / Hamstrings / Calves): Barbell Back Squat, Romanian Deadlift, Leg Extension, Standing Calf Raise
            """.trimIndent()
            SplitPreset.DOUBLE_MUSCLE -> """
                Day 1 · Back & Biceps: Pull-Up, Chest-Supported Row, Lat Pullover, Incline Dumbbell Curl, Dumbbell Hammer Curl
                Day 2 · Chest & Triceps: Barbell Bench Press, Incline Dumbbell Press, Cable Crossover, Overhead Cable Triceps Extension, Triceps Pushdown
                Day 3 · Legs & Shoulders (Repeat): Barbell Back Squat, Romanian Deadlift, Barbell Overhead Press, Dumbbell Lateral Raise, Standing Calf Raise
            """.trimIndent()
        }
        importSplitFromText(text)
    }

    suspend fun ensureSportExercisesSeeded() {
        exerciseDao.insertAll(SPORT_EXERCISES)
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
