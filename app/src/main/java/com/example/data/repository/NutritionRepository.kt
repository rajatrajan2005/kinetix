package com.example.data.repository

import com.example.data.local.dao.NutritionDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.remote.OpenFoodFactsClient
import com.example.domain.calculator.MacroCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.MacroTargets
import com.example.domain.model.MealSlot
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.roundToInt

class NutritionRepository(
    private val nutritionDao: NutritionDao,
    private val userProfileDao: UserProfileDao,
    private val offClient: OpenFoodFactsClient
) {
    fun observeTodayLogs(epochDay: Long = LocalDate.now().toEpochDay()): Flow<List<FoodLogEntity>> =
        nutritionDao.observeFoodLogsForDay(epochDay)

    fun observeRecentFoods(): Flow<List<FoodItemCacheEntity>> =
        nutritionDao.observeRecentFoods()

    fun observeCachedFoods(query: String): Flow<List<FoodItemCacheEntity>> =
        nutritionDao.observeCachedSearch(query.trim())

    fun observeMacroTargets(): Flow<MacroTargets> =
        userProfileDao.observeProfile().map { profile ->
            val p = profile ?: UserProfileEntity()
            MacroCalculator.calculate(
                age = p.age,
                sex = runCatching { Sex.valueOf(p.sex) }.getOrDefault(Sex.MALE),
                heightCm = p.heightCm,
                weightKg = p.weightKg,
                activityLevel = runCatching { ActivityLevel.valueOf(p.activityLevel) }
                    .getOrDefault(ActivityLevel.MODERATE),
                nutritionGoal = runCatching { NutritionGoal.valueOf(p.nutritionGoal) }
                    .getOrDefault(NutritionGoal.MAINTAIN),
                cutDeficitPct = p.cutDeficitPct,
                proteinPerKg = p.proteinPerKg
            )
        }

    /**
     * Network writes directly into Room cache; UI observes Room Flow for instant cached display.
     */
    suspend fun refreshRemoteSearch(query: String): Result<Int> {
        val result = offClient.searchFoods(query)
        result.onSuccess { items ->
            if (items.isNotEmpty()) {
                nutritionDao.upsertFoodCache(items)
            }
        }
        return result.map { it.size }
    }

    suspend fun logFood(
        food: FoodItemCacheEntity,
        grams: Double,
        mealSlot: MealSlot,
        epochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val ratio = grams.coerceAtLeast(1.0) / 100.0
        val now = System.currentTimeMillis()
        nutritionDao.upsertFoodCache(listOf(food.copy(lastUsedAtEpochMs = now)))
        nutritionDao.insertFoodLog(
            FoodLogEntity(
                dateEpochDay = epochDay,
                foodKey = food.barcodeOrKey,
                foodName = food.name,
                grams = grams,
                kcal = (food.kcalPer100g * ratio).roundToInt(),
                proteinG = food.proteinPer100g * ratio,
                carbsG = food.carbsPer100g * ratio,
                fatG = food.fatPer100g * ratio,
                mealSlot = mealSlot.name,
                loggedAtEpochMs = now
            )
        )
    }

    suspend fun addCustomFoodAndLog(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        gramsToLog: Double,
        mealSlot: MealSlot
    ) {
        val now = System.currentTimeMillis()
        val key = "custom_${name.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")}_$now"
        val item = FoodItemCacheEntity(
            barcodeOrKey = key,
            name = name.trim(),
            brand = "Custom",
            kcalPer100g = kcalPer100g.coerceAtLeast(0.0),
            proteinPer100g = proteinPer100g.coerceAtLeast(0.0),
            carbsPer100g = carbsPer100g.coerceAtLeast(0.0),
            fatPer100g = fatPer100g.coerceAtLeast(0.0),
            servingSizeG = gramsToLog.coerceAtLeast(1.0),
            cachedAtEpochMs = now,
            lastUsedAtEpochMs = now,
            isCustom = 1
        )
        logFood(item, gramsToLog, mealSlot)
    }

    /**
     * Logs a custom meal directly from total macros & calories (no per-100g conversion required).
     * Stores as a 1-serving item (servingSizeG = 100.0) in cache so quick re-logging reproduces the exact totals.
     */
    suspend fun addCustomMealTotalAndLog(
        name: String,
        totalKcal: Double,
        totalProteinG: Double,
        totalCarbsG: Double,
        totalFatG: Double,
        mealSlot: MealSlot,
        epochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val now = System.currentTimeMillis()
        val cleanName = name.trim()
        val key = "custom_total_${cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_")}_$now"
        val item = FoodItemCacheEntity(
            barcodeOrKey = key,
            name = cleanName,
            brand = "Custom Meal",
            kcalPer100g = totalKcal.coerceAtLeast(0.0),
            proteinPer100g = totalProteinG.coerceAtLeast(0.0),
            carbsPer100g = totalCarbsG.coerceAtLeast(0.0),
            fatPer100g = totalFatG.coerceAtLeast(0.0),
            servingSizeG = 100.0,
            cachedAtEpochMs = now,
            lastUsedAtEpochMs = now,
            isCustom = 1
        )
        nutritionDao.upsertFoodCache(listOf(item))
        nutritionDao.insertFoodLog(
            FoodLogEntity(
                dateEpochDay = epochDay,
                foodKey = key,
                foodName = cleanName,
                grams = 100.0,
                kcal = totalKcal.coerceAtLeast(0.0).roundToInt(),
                proteinG = totalProteinG.coerceAtLeast(0.0),
                carbsG = totalCarbsG.coerceAtLeast(0.0),
                fatG = totalFatG.coerceAtLeast(0.0),
                mealSlot = mealSlot.name,
                loggedAtEpochMs = now
            )
        )
    }

    suspend fun deleteFoodLog(logId: Long) {
        nutritionDao.deleteFoodLog(logId)
    }
}
