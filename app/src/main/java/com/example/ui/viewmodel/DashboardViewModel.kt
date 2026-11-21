package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FoodLogEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.StepRecordEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WaterLogEntity
import com.example.data.local.entity.WaterScheduleStateEntity
import com.example.data.repository.NutritionRepository
import com.example.data.repository.StepRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.WaterRepository
import com.example.data.repository.WorkoutRepository
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.MacroTargets
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.NutritionGoal
import com.example.domain.model.SetRepPrescription
import com.example.domain.model.Sex
import com.example.domain.model.TrainingGoal
import com.example.domain.model.WaterDoseSlot
import com.example.steps.StepSensorManager
import com.example.water.WaterAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

@Immutable
data class TodayUiState(
    val profile: UserProfileEntity = UserProfileEntity(),
    val macroTargets: MacroTargets = MacroCalculator.calculate(
        27, Sex.MALE, 178.0, 76.0, ActivityLevel.MODERATE, NutritionGoal.MAINTAIN
    ),
    val consumedKcal: Int = 0,
    val consumedProteinG: Double = 0.0,
    val consumedCarbsG: Double = 0.0,
    val consumedFatG: Double = 0.0,
    val waterConsumedMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val waterSchedule: WaterScheduleStateEntity? = null,
    val upcomingWaterSlots: List<WaterDoseSlot> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val stepsToday: Long = 0L,
    val stepGoal: Int = 9000,
    val isStepSensorAvailable: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val splitDays: List<SplitDayEntity> = emptyList(),
    val hasActiveWorkout: Boolean = false,
    val prescription: SetRepPrescription = TrainingTuningCalculator.prescriptionFor(TrainingGoal.HYPERTROPHY),
    val weeklyVolume: List<MuscleWeeklyVolume> = emptyList(),
    val recentSteps: List<StepRecordEntity> = emptyList()
)

class DashboardViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val nutritionRepository: NutritionRepository,
    private val waterRepository: WaterRepository,
    private val stepRepository: StepRepository,
    private val workoutRepository: WorkoutRepository,
    private val stepSensorManager: StepSensorManager,
    private val waterAlarmScheduler: WaterAlarmScheduler
) : ViewModel() {

    private val upcomingSlotsFlow = MutableStateFlow<List<WaterDoseSlot>>(emptyList())
    private val exactAlarmGrantedFlow = MutableStateFlow(waterAlarmScheduler.canScheduleExactAlarms())

    val uiState: StateFlow<TodayUiState> = combine(
        userProfileRepository.observeProfile(),
        nutritionRepository.observeTodayLogs(),
        waterRepository.observeTodayTotalMl(),
        waterRepository.observeTodaySchedule(),
        waterRepository.observeTodayLogs(),
        stepRepository.observeTodaySteps(),
        workoutRepository.observeSplitDays(),
        workoutRepository.observeActiveSession(),
        workoutRepository.observeWeeklyVolume(),
        stepRepository.observeRecentSteps(),
        upcomingSlotsFlow,
        exactAlarmGrantedFlow
    ) { values ->
        val profile = values[0] as UserProfileEntity
        @Suppress("UNCHECKED_CAST")
        val foodLogs = values[1] as List<FoodLogEntity>
        val waterMl = values[2] as Int
        val waterState = values[3] as WaterScheduleStateEntity?
        @Suppress("UNCHECKED_CAST")
        val waterLogs = values[4] as List<WaterLogEntity>
        val stepRecord = values[5] as StepRecordEntity?
        @Suppress("UNCHECKED_CAST")
        val splitDays = values[6] as List<SplitDayEntity>
        val activeSession = values[7]
        @Suppress("UNCHECKED_CAST")
        val weeklyVolume = values[8] as List<MuscleWeeklyVolume>
        @Suppress("UNCHECKED_CAST")
        val recentSteps = values[9] as List<StepRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val slots = values[10] as List<WaterDoseSlot>
        val canExact = values[11] as Boolean

        val sex = runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE)
        val activity = runCatching { ActivityLevel.valueOf(profile.activityLevel) }
            .getOrDefault(ActivityLevel.MODERATE)
        val nGoal = runCatching { NutritionGoal.valueOf(profile.nutritionGoal) }
            .getOrDefault(NutritionGoal.MAINTAIN)
        val tGoal = runCatching { TrainingGoal.valueOf(profile.trainingGoal) }
            .getOrDefault(TrainingGoal.HYPERTROPHY)

        val macros = MacroCalculator.calculate(
            age = profile.age,
            sex = sex,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            activityLevel = activity,
            nutritionGoal = nGoal,
            cutDeficitPct = profile.cutDeficitPct,
            proteinPerKg = profile.proteinPerKg
        )
        val waterTarget = waterState?.targetMl ?: WaterScheduleCalculator.dailyTargetMl(
            profile.weightKg,
            profile.waterMlPerKg
        )

        TodayUiState(
            profile = profile,
            macroTargets = macros,
            consumedKcal = foodLogs.sumOf { it.kcal },
            consumedProteinG = foodLogs.sumOf { it.proteinG },
            consumedCarbsG = foodLogs.sumOf { it.carbsG },
            consumedFatG = foodLogs.sumOf { it.fatG },
            waterConsumedMl = waterMl,
            waterTargetMl = waterTarget,
            waterSchedule = waterState,
            upcomingWaterSlots = slots,
            waterLogs = waterLogs,
            stepsToday = stepRecord?.stepsToday ?: 0L,
            stepGoal = profile.stepGoal,
            isStepSensorAvailable = stepSensorManager.isSensorAvailable(),
            canScheduleExactAlarms = canExact,
            splitDays = splitDays,
            hasActiveWorkout = activeSession != null,
            prescription = TrainingTuningCalculator.prescriptionFor(tGoal),
            weeklyVolume = weeklyVolume,
            recentSteps = recentSteps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    init {
        refreshWaterSlots()
    }

    fun startForegroundStepListener() {
        viewModelScope.launch {
            stepSensorManager.observeHardwareCounter().collect { counter ->
                stepRepository.recordHardwareCounter(counter)
            }
        }
    }

    fun refreshPermissionsState() {
        exactAlarmGrantedFlow.value = waterAlarmScheduler.canScheduleExactAlarms()
    }

    fun refreshWaterSlots() {
        viewModelScope.launch {
            upcomingSlotsFlow.value = waterRepository.computeTodaySlots()
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            waterRepository.logWater(amountMl, source = "app_quick_tap")
            upcomingSlotsFlow.value = waterRepository.computeTodaySlots()
        }
    }

    fun deleteWaterLog(id: Long) {
        viewModelScope.launch {
            waterRepository.deleteWaterLog(id)
            upcomingSlotsFlow.value = waterRepository.computeTodaySlots()
        }
    }

    fun markWakeUpNow() {
        viewModelScope.launch {
            waterRepository.markWakeUpNow(manualOverride = true)
            upcomingSlotsFlow.value = waterRepository.computeTodaySlots()
        }
    }

    fun setWakeTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            waterRepository.setWakeTimeMinutes((hour * 60) + minute, manualOverride = true)
            upcomingSlotsFlow.value = waterRepository.computeTodaySlots()
        }
    }

    fun completeOnboarding(
        age: Int,
        sex: Sex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        nutritionGoal: NutritionGoal,
        trainingGoal: TrainingGoal,
        wakeTime: LocalTime,
        bedTime: LocalTime
    ) {
        viewModelScope.launch {
            val current = userProfileRepository.getProfileOnce()
            userProfileRepository.saveProfile(
                current.copy(
                    age = age,
                    sex = sex.name,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    activityLevel = activityLevel.name,
                    nutritionGoal = nutritionGoal.name,
                    trainingGoal = trainingGoal.name,
                    wakeTimeMinutes = wakeTime.hour * 60 + wakeTime.minute,
                    bedTimeMinutes = bedTime.hour * 60 + bedTime.minute,
                    hasCompletedOnboarding = 1
                )
            )
            waterRepository.rescheduleFromCurrentState()
            refreshWaterSlots()
        }
    }

    class Factory(
        private val userProfileRepository: UserProfileRepository,
        private val nutritionRepository: NutritionRepository,
        private val waterRepository: WaterRepository,
        private val stepRepository: StepRepository,
        private val workoutRepository: WorkoutRepository,
        private val stepSensorManager: StepSensorManager,
        private val waterAlarmScheduler: WaterAlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                userProfileRepository,
                nutritionRepository,
                waterRepository,
                stepRepository,
                workoutRepository,
                stepSensorManager,
                waterAlarmScheduler
            ) as T
        }
    }
}
