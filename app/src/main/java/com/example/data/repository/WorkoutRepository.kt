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
        val starterText = """
            Push (Chest / Delts / Triceps): Barbell Bench Press, Incline Dumbbell Press, Dumbbell Lateral Raise, Overhead Cable Triceps Extension
            Pull (Back / Rear Delts / Biceps): Pull-Up, Chest-Supported Row, Face Pull, Incline Dumbbell Curl
            Legs (Quads / Hamstrings / Calves): Barbell Back Squat, Romanian Deadlift, Leg Extension, Standing Calf Raise
        """.trimIndent()
        importSplitFromText(starterText)
    }
}
