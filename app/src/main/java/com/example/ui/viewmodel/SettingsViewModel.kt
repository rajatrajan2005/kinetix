package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CycleLogEntity
import com.example.data.local.entity.ProgressPhotoEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.ProgressAndCycleRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.WaterRepository
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository,
    private val progressAndCycleRepository: ProgressAndCycleRepository
) : ViewModel() {

    val profileState: StateFlow<UserProfileEntity> = userProfileRepository.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfileEntity()
        )

    val progressPhotos: StateFlow<List<ProgressPhotoEntity>> =
        progressAndCycleRepository.observeProgressPhotos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val cycleLogs: StateFlow<List<CycleLogEntity>> =
        progressAndCycleRepository.observeCycleLogs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun updateProfile(
        displayName: String? = null,
        age: Int? = null,
        sex: Sex? = null,
        heightCm: Double? = null,
        weightKg: Double? = null,
        activityLevel: ActivityLevel? = null,
        nutritionGoal: NutritionGoal? = null,
        trainingGoal: TrainingGoal? = null,
        cutDeficitPct: Double? = null,
        proteinPerKg: Double? = null,
        wakeTimeMinutes: Int? = null,
        bedTimeMinutes: Int? = null,
        waterMlPerKg: Double? = null,
        stepGoal: Int? = null,
        trackCycle: Boolean? = null,
        cycleLengthDays: Int? = null,
        periodLengthDays: Int? = null,
        sportType: SportDiscipline? = null
    ) {
        viewModelScope.launch {
            val cur = userProfileRepository.getProfileOnce()
            val nextSex = sex ?: runCatching { Sex.valueOf(cur.sex) }.getOrDefault(Sex.MALE)
            val resolvedTrackCycle = when {
                nextSex == Sex.MALE -> 0
                trackCycle != null -> if (trackCycle) 1 else 0
                else -> cur.trackCycle
            }

            val goalPreset = trainingGoal?.let { TrainingTuningCalculator.recommendedTargetsFor(it) }
            val nextNutritionGoal = nutritionGoal?.name
                ?: goalPreset?.nutritionGoal?.name
                ?: cur.nutritionGoal
            val nextProteinPerKg = proteinPerKg
                ?: goalPreset?.proteinPerKg
                ?: cur.proteinPerKg
            val nextWaterMlPerKg = waterMlPerKg
                ?: goalPreset?.waterMlPerKg
                ?: cur.waterMlPerKg
            val nextStepGoal = stepGoal
                ?: goalPreset?.stepGoal
                ?: cur.stepGoal

            val updated = cur.copy(
                displayName = displayName?.take(40) ?: cur.displayName,
                age = age ?: cur.age,
                sex = nextSex.name,
                heightCm = heightCm ?: cur.heightCm,
                weightKg = weightKg ?: cur.weightKg,
                activityLevel = activityLevel?.name ?: cur.activityLevel,
                nutritionGoal = nextNutritionGoal,
                trainingGoal = trainingGoal?.name ?: cur.trainingGoal,
                cutDeficitPct = cutDeficitPct ?: cur.cutDeficitPct,
                proteinPerKg = nextProteinPerKg,
                wakeTimeMinutes = wakeTimeMinutes ?: cur.wakeTimeMinutes,
                bedTimeMinutes = bedTimeMinutes ?: cur.bedTimeMinutes,
                manualWakeOverride = if (wakeTimeMinutes != null) 1 else cur.manualWakeOverride,
                waterMlPerKg = nextWaterMlPerKg,
                stepGoal = nextStepGoal,
                trackCycle = resolvedTrackCycle,
                cycleLengthDays = cycleLengthDays?.coerceIn(20, 45) ?: cur.cycleLengthDays,
                periodLengthDays = periodLengthDays?.coerceIn(2, 10) ?: cur.periodLengthDays,
                sportType = sportType?.name ?: cur.sportType
            )
            userProfileRepository.saveProfile(updated)
            waterRepository.rescheduleFromCurrentState()
        }
    }

    fun saveProfileAvatar(uri: Uri) {
        viewModelScope.launch {
            progressAndCycleRepository.saveProfileAvatar(uri)
        }
    }

    fun saveProfileAvatarBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            progressAndCycleRepository.saveProfileAvatarBitmap(bitmap)
        }
    }

    fun addProgressPhoto(uri: Uri, weekLabel: String, weightKgSnapshot: Double, notes: String) {
        viewModelScope.launch {
            progressAndCycleRepository.addProgressPhoto(
                uri = uri,
                weekLabel = weekLabel,
                weightKgSnapshot = weightKgSnapshot,
                notes = notes
            )
        }
    }

    fun deleteProgressPhoto(photo: ProgressPhotoEntity) {
        viewModelScope.launch {
            progressAndCycleRepository.deleteProgressPhoto(photo)
        }
    }

    fun logCycle(
        startDateEpochDay: Long,
        endDateEpochDay: Long?,
        flowIntensity: String,
        symptomsCsv: String,
        notes: String
    ) {
        viewModelScope.launch {
            progressAndCycleRepository.logCycle(
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                flowIntensity = flowIntensity,
                symptomsCsv = symptomsCsv,
                notes = notes
            )
        }
    }

    fun deleteCycleLog(id: Long) {
        viewModelScope.launch {
            progressAndCycleRepository.deleteCycleLog(id)
        }
    }

    class Factory(
        private val userProfileRepository: UserProfileRepository,
        private val waterRepository: WaterRepository,
        private val progressAndCycleRepository: ProgressAndCycleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userProfileRepository,
                waterRepository,
                progressAndCycleRepository
            ) as T
        }
    }
}
