package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.data.repository.NutritionRepository
import com.example.domain.calculator.MacroCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.MacroTargets
import com.example.domain.model.MealSlot
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class NutritionUiState(
    val targets: MacroTargets = MacroCalculator.calculate(
        27, Sex.MALE, 178.0, 76.0, ActivityLevel.MODERATE, NutritionGoal.MAINTAIN
    ),
    val todayLogs: List<FoodLogEntity> = emptyList(),
    val recentFoods: List<FoodItemCacheEntity> = emptyList(),
    val searchResults: List<FoodItemCacheEntity> = emptyList(),
    val query: String = "",
    val isRefreshingRemote: Boolean = false,
    val remoteError: String? = null
) {
    val consumedKcal: Int get() = todayLogs.sumOf { it.kcal }
    val consumedProteinG: Double get() = todayLogs.sumOf { it.proteinG }
    val consumedCarbsG: Double get() = todayLogs.sumOf { it.carbsG }
    val consumedFatG: Double get() = todayLogs.sumOf { it.fatG }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val refreshingFlow = MutableStateFlow(false)
    private val remoteErrorFlow = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    private val cachedSearchFlow = queryFlow.flatMapLatest { q ->
        nutritionRepository.observeCachedFoods(q)
    }

    val uiState: StateFlow<NutritionUiState> = combine(
        nutritionRepository.observeMacroTargets(),
        nutritionRepository.observeTodayLogs(),
        nutritionRepository.observeRecentFoods(),
        cachedSearchFlow,
        queryFlow,
        refreshingFlow,
        remoteErrorFlow
    ) { arr ->
        val targets = arr[0] as MacroTargets
        @Suppress("UNCHECKED_CAST")
        val logs = arr[1] as List<FoodLogEntity>
        @Suppress("UNCHECKED_CAST")
        val recents = arr[2] as List<FoodItemCacheEntity>
        @Suppress("UNCHECKED_CAST")
        val cached = arr[3] as List<FoodItemCacheEntity>
        val q = arr[4] as String
        val refreshing = arr[5] as Boolean
        val error = arr[6] as String?

        NutritionUiState(
            targets = targets,
            todayLogs = logs,
            recentFoods = recents,
            searchResults = cached,
            query = q,
            isRefreshingRemote = refreshing,
            remoteError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState()
    )

    fun onQueryChanged(newQuery: String) {
        queryFlow.value = newQuery
        remoteErrorFlow.value = null
        searchJob?.cancel()
        if (newQuery.trim().length < 2) {
            refreshingFlow.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350L)
            refreshingFlow.value = true
            val result = nutritionRepository.refreshRemoteSearch(newQuery)
            result.onFailure { err ->
                remoteErrorFlow.value = err.message ?: "Offline · showing cached items"
            }
            refreshingFlow.value = false
        }
    }

    fun triggerImmediateRemoteSearch() {
        val q = queryFlow.value.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            refreshingFlow.value = true
            remoteErrorFlow.value = null
            val result = nutritionRepository.refreshRemoteSearch(q)
            result.onFailure { err ->
                remoteErrorFlow.value = err.message ?: "Offline · showing cached items"
            }
            refreshingFlow.value = false
        }
    }

    fun logFood(food: FoodItemCacheEntity, grams: Double, mealSlot: MealSlot) {
        viewModelScope.launch {
            nutritionRepository.logFood(food, grams, mealSlot)
        }
    }

    fun addCustomFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        gramsToLog: Double,
        mealSlot: MealSlot
    ) {
        viewModelScope.launch {
            nutritionRepository.addCustomFoodAndLog(
                name,
                kcalPer100g,
                proteinPer100g,
                carbsPer100g,
                fatPer100g,
                gramsToLog,
                mealSlot
            )
        }
    }

    fun addCustomMealTotals(
        name: String,
        totalKcal: Double,
        totalProteinG: Double,
        totalCarbsG: Double,
        totalFatG: Double,
        mealSlot: MealSlot
    ) {
        viewModelScope.launch {
            nutritionRepository.addCustomMealTotalAndLog(
                name = name,
                totalKcal = totalKcal,
                totalProteinG = totalProteinG,
                totalCarbsG = totalCarbsG,
                totalFatG = totalFatG,
                mealSlot = mealSlot
            )
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            nutritionRepository.deleteFoodLog(logId)
        }
    }

    class Factory(
        private val nutritionRepository: NutritionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NutritionViewModel(nutritionRepository) as T
        }
    }
}
