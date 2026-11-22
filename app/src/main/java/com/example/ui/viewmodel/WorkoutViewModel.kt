package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.SplitExerciseEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutSetEntity
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.WorkoutRepository
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.OverloadSuggestion
import com.example.domain.model.SetRepPrescription
import com.example.domain.model.SplitPreset
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import com.example.media.MediaControlRepository
import com.example.media.MediaPlaybackUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ExerciseSessionConfig(
    val targetSets: Int = 3,
    val repMin: Int = 6,
    val repMax: Int = 12,
    val restSeconds: Int = 90,
    val weightStepKg: Double = 2.5,
    val autoStartRest: Boolean = true,
    val isWarmupNext: Boolean = false,
    val note: String = ""
)

@Immutable
data class ActiveExerciseBlock(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val muscleHead: String,
    val isAdHoc: Boolean,
    val loggedSets: List<WorkoutSetEntity>,
    val previousSets: List<WorkoutSetEntity>,
    val overloadSuggestion: OverloadSuggestion?,
    val suggestedWeightKg: Double,
    val suggestedReps: Int,
    val config: ExerciseSessionConfig
)

@Immutable
data class RestTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 90
)

@Immutable
data class WorkoutUiState(
    val trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val prescription: SetRepPrescription = TrainingTuningCalculator.prescriptionFor(TrainingGoal.HYPERTROPHY),
    val sportDiscipline: SportDiscipline = SportDiscipline.NONE,
    val sportRecommendedExercises: List<ExerciseEntity> = emptyList(),
    val splitDays: List<SplitDayEntity> = emptyList(),
    val splitExercisesByDay: Map<Long, List<SplitExerciseEntity>> = emptyMap(),
    val activeSession: WorkoutSessionEntity? = null,
    val activeBlocks: List<ActiveExerciseBlock> = emptyList(),
    val selectedActiveExerciseId: String? = null,
    val completedSessions: List<WorkoutSessionEntity> = emptyList(),
    val historySetsBySession: Map<Long, List<WorkoutSetEntity>> = emptyMap(),
    val weeklyVolume: List<MuscleWeeklyVolume> = emptyList(),
    val restTimer: RestTimerState = RestTimerState(),
    val mediaState: MediaPlaybackUiState = MediaPlaybackUiState(),
    val pendingMakePermanentExercise: ExerciseEntity? = null,
    val inspectedExercise: ExerciseEntity? = null,
    val muscleGroups: List<String> = emptyList(),
    val muscleHeads: List<String> = emptyList(),
    val selectedMuscle: String = "",
    val selectedHead: String = "",
    val pickerQuery: String = "",
    val filteredExercises: List<ExerciseEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository,
    private val mediaControlRepository: MediaControlRepository
) : ViewModel() {

    private val adHocExercisesFlow = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private val hiddenExerciseIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val sessionConfigsFlow = MutableStateFlow<Map<String, ExerciseSessionConfig>>(emptyMap())
    private val selectedActiveExerciseIdFlow = MutableStateFlow<String?>(null)
    private val previousCacheFlow =
        MutableStateFlow<Map<String, Pair<List<WorkoutSetEntity>, OverloadSuggestion?>>>(emptyMap())
    private val restTimerFlow = MutableStateFlow(RestTimerState())
    private val pendingPermanentFlow = MutableStateFlow<ExerciseEntity?>(null)
    private val inspectedExerciseFlow = MutableStateFlow<ExerciseEntity?>(null)

    private val selectedMuscleFlow = MutableStateFlow("")
    private val selectedHeadFlow = MutableStateFlow("")
    private val pickerQueryFlow = MutableStateFlow("")

    private var timerJob: Job? = null

    private val activeSetsFlow = workoutRepository.observeActiveSession().flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else workoutRepository.observeSetsForSession(session.id)
    }

    private val muscleHeadsFlow = selectedMuscleFlow.flatMapLatest { muscle ->
        if (muscle.isBlank()) flowOf(emptyList()) else workoutRepository.observeHeadsForMuscle(muscle)
    }

    private val filteredExercisesFlow = combine(
        selectedMuscleFlow,
        selectedHeadFlow,
        pickerQueryFlow
    ) { m, h, q -> Triple(m, h, q) }.flatMapLatest { (m, h, q) ->
        workoutRepository.observeFilteredExercises(m, h, q)
    }

    private val sessionCustomizationFlow = combine(
        adHocExercisesFlow,
        hiddenExerciseIdsFlow,
        sessionConfigsFlow,
        selectedActiveExerciseIdFlow,
        inspectedExerciseFlow
    ) { adHoc, hidden, configs, selId, inspected ->
        SessionCustomState(adHoc, hidden, configs, selId, inspected)
    }

    private data class SessionCustomState(
        val adHoc: List<ExerciseEntity>,
        val hiddenIds: Set<String>,
        val configs: Map<String, ExerciseSessionConfig>,
        val selectedActiveId: String?,
        val inspectedExercise: ExerciseEntity?
    )

    val uiState: StateFlow<WorkoutUiState> = combine(
        userProfileRepository.observeProfile(),
        workoutRepository.observeSplitDays(),
        workoutRepository.observeAllSplitExercises(),
        workoutRepository.observeActiveSession(),
        activeSetsFlow,
        workoutRepository.observeCompletedSessions(),
        workoutRepository.observeRecentHistorySets(),
        workoutRepository.observeWeeklyVolume(),
        sessionCustomizationFlow,
        previousCacheFlow,
        restTimerFlow,
        mediaControlRepository.state,
        pendingPermanentFlow,
        workoutRepository.observeMuscleGroups(),
        muscleHeadsFlow,
        filteredExercisesFlow,
        selectedMuscleFlow,
        selectedHeadFlow,
        pickerQueryFlow,
        workoutRepository.observeAllExercises()
    ) { arr ->
        val profile = arr[0] as UserProfileEntity
        @Suppress("UNCHECKED_CAST")
        val splitDays = arr[1] as List<SplitDayEntity>
        @Suppress("UNCHECKED_CAST")
        val splitExercises = arr[2] as List<SplitExerciseEntity>
        val activeSession = arr[3] as WorkoutSessionEntity?
        @Suppress("UNCHECKED_CAST")
        val activeSets = arr[4] as List<WorkoutSetEntity>
        @Suppress("UNCHECKED_CAST")
        val completedSessions = arr[5] as List<WorkoutSessionEntity>
        @Suppress("UNCHECKED_CAST")
        val historySets = arr[6] as List<WorkoutSetEntity>
        @Suppress("UNCHECKED_CAST")
        val weeklyVolume = arr[7] as List<MuscleWeeklyVolume>
        val customState = arr[8] as SessionCustomState
        @Suppress("UNCHECKED_CAST")
        val prevMap = arr[9] as Map<String, Pair<List<WorkoutSetEntity>, OverloadSuggestion?>>
        val restTimer = arr[10] as RestTimerState
        val mediaState = arr[11] as MediaPlaybackUiState
        val pendingPermanent = arr[12] as ExerciseEntity?
        @Suppress("UNCHECKED_CAST")
        val muscleGroups = arr[13] as List<String>
        @Suppress("UNCHECKED_CAST")
        val muscleHeads = arr[14] as List<String>
        @Suppress("UNCHECKED_CAST")
        val filteredExercises = arr[15] as List<ExerciseEntity>
        val selMuscle = arr[16] as String
        val selHead = arr[17] as String
        val query = arr[18] as String
        @Suppress("UNCHECKED_CAST")
        val allExercises = arr[19] as List<ExerciseEntity>

        val goal = runCatching { TrainingGoal.valueOf(profile.trainingGoal) }
            .getOrDefault(TrainingGoal.HYPERTROPHY)
        val sport = runCatching { SportDiscipline.valueOf(profile.sportType) }
            .getOrDefault(SportDiscipline.NONE)
        val sportExercises = workoutRepository.recommendedForSport(sport, allExercises)
        val rx = TrainingTuningCalculator.prescriptionFor(goal)
        val byDay = splitExercises.groupBy { it.splitDayId }

        val activeBlocks = if (activeSession != null) {
            val baseItems = byDay[activeSession.splitDayId].orEmpty().map {
                ActiveStub(
                    id = it.exerciseId,
                    name = it.exerciseName,
                    muscleGroup = it.muscleGroup,
                    muscleHead = it.muscleHead,
                    isAdHoc = false,
                    targetSets = it.targetSets,
                    repMin = it.repMin,
                    repMax = it.repMax,
                    restSeconds = it.restSeconds
                )
            }
            val adHocItems = customState.adHoc.map {
                ActiveStub(it.id, it.name, it.muscleGroup, it.muscleHead, true)
            }
            val setExtraItems = activeSets
                .filter { s ->
                    baseItems.none { it.id == s.exerciseId } && adHocItems.none { it.id == s.exerciseId }
                }
                .distinctBy { it.exerciseId }
                .map { ActiveStub(it.exerciseId, it.exerciseName, it.muscleGroup, it.muscleHead, it.isAdHoc) }

            (baseItems + adHocItems + setExtraItems)
                .distinctBy { it.id }
                .filter { it.id !in customState.hiddenIds }
                .map { stub ->
                    val sets = activeSets.filter { it.exerciseId == stub.id }
                    val prevData = prevMap[stub.id]
                    val prevSets = prevData?.first.orEmpty()
                    val overload = prevData?.second
                    val nextIndex = sets.size
                    val matchingPrev = prevSets.getOrNull(nextIndex) ?: prevSets.lastOrNull()
                    val defaultWeight = overload?.takeIf { it.shouldIncrease }?.suggestedWeightKg
                        ?: matchingPrev?.weightKg
                        ?: 20.0
                    val cfg = customState.configs[stub.id] ?: ExerciseSessionConfig(
                        targetSets = if (stub.targetSets > 0) stub.targetSets else rx.sets,
                        repMin = if (stub.repMin > 0) stub.repMin else rx.repMin,
                        repMax = if (stub.repMax > 0) stub.repMax else rx.repMax,
                        restSeconds = if (stub.restSeconds > 0) stub.restSeconds else rx.restSeconds
                    )
                    val defaultReps = if (overload?.shouldIncrease == true) cfg.repMin else (matchingPrev?.reps ?: cfg.repMax)

                    ActiveExerciseBlock(
                        exerciseId = stub.id,
                        exerciseName = stub.name,
                        muscleGroup = stub.muscleGroup,
                        muscleHead = stub.muscleHead,
                        isAdHoc = stub.isAdHoc,
                        loggedSets = sets,
                        previousSets = prevSets,
                        overloadSuggestion = overload,
                        suggestedWeightKg = defaultWeight,
                        suggestedReps = defaultReps,
                        config = cfg
                    )
                }
        } else {
            emptyList()
        }

        val resolvedActiveId = customState.selectedActiveId
            ?.takeIf { id -> activeBlocks.any { it.exerciseId == id } }
            ?: activeBlocks.firstOrNull()?.exerciseId

        WorkoutUiState(
            trainingGoal = goal,
            prescription = rx,
            sportDiscipline = sport,
            sportRecommendedExercises = sportExercises,
            splitDays = splitDays,
            splitExercisesByDay = byDay,
            activeSession = activeSession,
            activeBlocks = activeBlocks,
            selectedActiveExerciseId = resolvedActiveId,
            completedSessions = completedSessions,
            historySetsBySession = historySets.groupBy { it.sessionId },
            weeklyVolume = weeklyVolume,
            restTimer = restTimer,
            mediaState = mediaState,
            pendingMakePermanentExercise = pendingPermanent,
            inspectedExercise = customState.inspectedExercise,
            muscleGroups = muscleGroups,
            muscleHeads = muscleHeads,
            selectedMuscle = selMuscle,
            selectedHead = selHead,
            pickerQuery = query,
            filteredExercises = filteredExercises
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState()
    )

    init {
        viewModelScope.launch {
            workoutRepository.ensureSportExercisesSeeded()
            workoutRepository.ensureStarterSplitIfEmpty()
            workoutRepository.repairSplitAndExerciseTags()
        }
    }

    private data class ActiveStub(
        val id: String,
        val name: String,
        val muscleGroup: String,
        val muscleHead: String,
        val isAdHoc: Boolean,
        val targetSets: Int = 0,
        val repMin: Int = 0,
        val repMax: Int = 0,
        val restSeconds: Int = 0
    )

    fun refreshMediaBinding() = mediaControlRepository.refreshBinding()
    fun mediaPlayPause() = mediaControlRepository.playPause()
    fun mediaNext() = mediaControlRepository.skipNext()
    fun mediaPrevious() = mediaControlRepository.skipPrevious()
    fun mediaSeekTo(posMs: Long) = mediaControlRepository.seekTo(posMs)

    fun startWorkout(day: SplitDayEntity) {
        viewModelScope.launch {
            val goal = uiState.value.trainingGoal
            workoutRepository.startSession(day, goal)
            adHocExercisesFlow.value = emptyList()
            hiddenExerciseIdsFlow.value = emptySet()
            sessionConfigsFlow.value = emptyMap()
            val exercises = uiState.value.splitExercisesByDay[day.id].orEmpty()
            selectedActiveExerciseIdFlow.value = exercises.firstOrNull()?.exerciseId
            preloadPreviousData(exercises, goal)
        }
    }

    private suspend fun preloadPreviousData(exercises: List<SplitExerciseEntity>, goal: TrainingGoal) {
        val updated = previousCacheFlow.value.toMutableMap()
        exercises.forEach { ex ->
            updated[ex.exerciseId] = workoutRepository.getPreviousSessionData(
                exerciseId = ex.exerciseId,
                goal = goal,
                targetSetsOverride = ex.targetSets,
                repMaxOverride = ex.repMax
            )
        }
        previousCacheFlow.value = updated
    }

    private suspend fun preloadPreviousDataByIds(exerciseIds: List<String>, goal: TrainingGoal) {
        val updated = previousCacheFlow.value.toMutableMap()
        exerciseIds.forEach { id ->
            updated[id] = workoutRepository.getPreviousSessionData(id, goal)
        }
        previousCacheFlow.value = updated
    }

    fun selectActiveExercise(exerciseId: String) {
        selectedActiveExerciseIdFlow.value = exerciseId
    }

    fun updateExerciseSessionConfig(exerciseId: String, config: ExerciseSessionConfig) {
        sessionConfigsFlow.value = sessionConfigsFlow.value + (exerciseId to config)
    }

    fun skipExerciseForToday(exerciseId: String) {
        hiddenExerciseIdsFlow.value = hiddenExerciseIdsFlow.value + exerciseId
    }

    fun inspectExerciseById(exerciseId: String, fallbackName: String = "", fallbackMuscle: String = "", fallbackHead: String = "") {
        viewModelScope.launch {
            val fromDb = workoutRepository.getExerciseById(exerciseId)
            inspectedExerciseFlow.value = fromDb ?: ExerciseEntity(
                id = exerciseId,
                name = fallbackName,
                muscleGroup = fallbackMuscle,
                muscleHead = fallbackHead.ifBlank { fallbackMuscle },
                secondaryMuscles = "",
                equipment = "Gym Equipment",
                level = "Intermediate",
                mechanic = "Compound",
                instructions = "Control the eccentric and drive through a full range of motion."
            )
        }
    }

    fun dismissExerciseInspection() {
        inspectedExerciseFlow.value = null
    }

    fun logSet(
        exerciseId: String,
        exerciseName: String,
        muscleGroup: String,
        muscleHead: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        isAdHoc: Boolean,
        isWarmup: Boolean = false
    ) {
        val sessionId = uiState.value.activeSession?.id ?: return
        val blockConfig = uiState.value.activeBlocks
            .firstOrNull { it.exerciseId == exerciseId }
            ?.config
        val restSecs = blockConfig?.restSeconds ?: uiState.value.prescription.restSeconds
        val autoRest = blockConfig?.autoStartRest ?: true

        viewModelScope.launch {
            workoutRepository.logSet(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                muscleHead = muscleHead,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                isWarmup = isWarmup,
                isAdHoc = isAdHoc
            )
            if (isWarmup && blockConfig != null) {
                updateExerciseSessionConfig(exerciseId, blockConfig.copy(isWarmupNext = false))
            }
            if (autoRest) {
                startRestTimer(restSecs)
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { workoutRepository.deleteSet(setId) }
    }

    fun finishWorkout() {
        val sessionId = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            workoutRepository.finishSession(sessionId)
            adHocExercisesFlow.value = emptyList()
            hiddenExerciseIdsFlow.value = emptySet()
            sessionConfigsFlow.value = emptyMap()
            selectedActiveExerciseIdFlow.value = null
            pendingPermanentFlow.value = null
            stopRestTimer()
        }
    }

    fun discardWorkout() {
        val sessionId = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            workoutRepository.discardSession(sessionId)
            adHocExercisesFlow.value = emptyList()
            hiddenExerciseIdsFlow.value = emptySet()
            sessionConfigsFlow.value = emptyMap()
            selectedActiveExerciseIdFlow.value = null
            pendingPermanentFlow.value = null
            stopRestTimer()
        }
    }

    fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        val safeSeconds = seconds.coerceAtLeast(1)
        restTimerFlow.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = safeSeconds,
            totalSeconds = safeSeconds
        )
        launchTimerCountdown(safeSeconds, safeSeconds)
    }

    private fun launchTimerCountdown(startFrom: Int, total: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = startFrom
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                restTimerFlow.value = RestTimerState(
                    isRunning = remaining > 0,
                    isPaused = false,
                    remainingSeconds = remaining,
                    totalSeconds = total
                )
            }
        }
    }

    fun toggleRestTimerPause() {
        val cur = restTimerFlow.value
        if (cur.remainingSeconds <= 0) return
        if (cur.isPaused) {
            restTimerFlow.value = cur.copy(isRunning = true, isPaused = false)
            launchTimerCountdown(cur.remainingSeconds, cur.totalSeconds)
        } else {
            timerJob?.cancel()
            restTimerFlow.value = cur.copy(isRunning = false, isPaused = true)
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = restTimerFlow.value
        val next = (current.remainingSeconds + deltaSeconds).coerceAtLeast(0)
        if (next == 0) {
            stopRestTimer()
        } else {
            val newTotal = maxOf(current.totalSeconds, next)
            restTimerFlow.value = RestTimerState(
                isRunning = !current.isPaused,
                isPaused = current.isPaused,
                remainingSeconds = next,
                totalSeconds = newTotal
            )
            if (!current.isPaused) {
                launchTimerCountdown(next, newTotal)
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        restTimerFlow.value = RestTimerState(
            isRunning = false,
            isPaused = false,
            remainingSeconds = 0,
            totalSeconds = uiState.value.prescription.restSeconds
        )
    }

    fun selectMuscleFilter(muscle: String) {
        selectedMuscleFlow.value = if (selectedMuscleFlow.value == muscle) "" else muscle
        selectedHeadFlow.value = ""
    }

    fun selectHeadFilter(head: String) {
        selectedHeadFlow.value = if (selectedHeadFlow.value == head) "" else head
    }

    fun updatePickerQuery(query: String) {
        pickerQueryFlow.value = query
    }

    fun addAdHocExerciseToToday(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (uiState.value.activeSession == null) {
                val firstDay = uiState.value.splitDays.firstOrNull()
                if (firstDay != null) {
                    workoutRepository.startSession(firstDay, uiState.value.trainingGoal)
                }
            }
            hiddenExerciseIdsFlow.value = hiddenExerciseIdsFlow.value - exercise.id
            if (adHocExercisesFlow.value.none { it.id == exercise.id }) {
                adHocExercisesFlow.value = adHocExercisesFlow.value + exercise
            }
            selectedActiveExerciseIdFlow.value = exercise.id
            preloadPreviousDataByIds(listOf(exercise.id), uiState.value.trainingGoal)
            pendingPermanentFlow.value = exercise
        }
    }

    fun confirmMakePermanent(exercise: ExerciseEntity) {
        val splitDayId = uiState.value.activeSession?.splitDayId
            ?: uiState.value.splitDays.firstOrNull()?.id
            ?: return
        viewModelScope.launch {
            workoutRepository.addExerciseToSplitDay(splitDayId, exercise)
            pendingPermanentFlow.value = null
        }
    }

    fun dismissMakePermanent() {
        pendingPermanentFlow.value = null
    }

    fun addExerciseToSplitDay(splitDayId: Long, exercise: ExerciseEntity) {
        viewModelScope.launch { workoutRepository.addExerciseToSplitDay(splitDayId, exercise) }
    }

    fun removeExerciseFromSplit(splitExerciseId: Long) {
        viewModelScope.launch { workoutRepository.removeExerciseFromSplit(splitExerciseId) }
    }

    fun addSplitDay(name: String, muscles: String) {
        viewModelScope.launch { workoutRepository.addSplitDay(name, muscles) }
    }

    fun addRestDay() {
        viewModelScope.launch { workoutRepository.addRestDay() }
    }

    fun updateSplitDay(dayId: Long, name: String, muscles: String) {
        viewModelScope.launch { workoutRepository.updateSplitDay(dayId, name, muscles) }
    }

    fun moveSplitExercise(splitDayId: Long, splitExerciseId: Long, moveUp: Boolean) {
        viewModelScope.launch { workoutRepository.moveSplitExercise(splitDayId, splitExerciseId, moveUp) }
    }

    fun reorderSplitExercises(splitDayId: Long, orderedExerciseIds: List<Long>) {
        viewModelScope.launch { workoutRepository.reorderSplitExercises(splitDayId, orderedExerciseIds) }
    }

    fun deleteSplitDay(dayId: Long) {
        viewModelScope.launch { workoutRepository.deleteSplitDay(dayId) }
    }

    fun importSplitText(rawText: String) {
        viewModelScope.launch { workoutRepository.importSplitFromText(rawText) }
    }

    fun applySplitPreset(preset: SplitPreset) {
        viewModelScope.launch { workoutRepository.applySplitPreset(preset) }
    }

    fun selectSport(sport: SportDiscipline) {
        viewModelScope.launch {
            workoutRepository.ensureSportExercisesSeeded()
            val cur = userProfileRepository.getProfileOnce()
            userProfileRepository.saveProfile(cur.copy(sportType = sport.name))
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val userProfileRepository: UserProfileRepository,
        private val mediaControlRepository: MediaControlRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(workoutRepository, userProfileRepository, mediaControlRepository) as T
        }
    }
}
