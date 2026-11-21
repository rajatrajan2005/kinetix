package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.WaterRepository
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.TrainingGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository
) : ViewModel() {

    val profileState: StateFlow<UserProfileEntity> = userProfileRepository.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfileEntity()
        )

    fun updateProfile(
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
        stepGoal: Int? = null
    ) {
        viewModelScope.launch {
            val cur = userProfileRepository.getProfileOnce()
            val updated = cur.copy(
                age = age ?: cur.age,
                sex = sex?.name ?: cur.sex,
                heightCm = heightCm ?: cur.heightCm,
                weightKg = weightKg ?: cur.weightKg,
                activityLevel = activityLevel?.name ?: cur.activityLevel,
                nutritionGoal = nutritionGoal?.name ?: cur.nutritionGoal,
                trainingGoal = trainingGoal?.name ?: cur.trainingGoal,
                cutDeficitPct = cutDeficitPct ?: cur.cutDeficitPct,
                proteinPerKg = proteinPerKg ?: cur.proteinPerKg,
                wakeTimeMinutes = wakeTimeMinutes ?: cur.wakeTimeMinutes,
                bedTimeMinutes = bedTimeMinutes ?: cur.bedTimeMinutes,
                manualWakeOverride = if (wakeTimeMinutes != null) 1 else cur.manualWakeOverride,
                waterMlPerKg = waterMlPerKg ?: cur.waterMlPerKg,
                stepGoal = stepGoal ?: cur.stepGoal
            )
            userProfileRepository.saveProfile(updated)
            waterRepository.rescheduleFromCurrentState()
        }
    }

    class Factory(
        private val userProfileRepository: UserProfileRepository,
        private val waterRepository: WaterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userProfileRepository, waterRepository) as T
        }
    }
}
