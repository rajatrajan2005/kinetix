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

    fun observeCustomFoods(): Flow<List<FoodItemCacheEntity>> =
        nutritionDao.observeCustomFoods()

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

    suspend fun saveCustomFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        gramsToLog: Double,
        mealSlot: MealSlot,
        alsoLog: Boolean,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): FoodItemCacheEntity {
        val now = System.currentTimeMillis()
        val cleanName = name.trim().ifBlank { "Custom Ingredient" }
        val p = proteinPer100g.coerceAtLeast(0.0)
        val c = carbsPer100g.coerceAtLeast(0.0)
        val f = fatPer100g.coerceAtLeast(0.0)
        val resolvedKcal = if (kcalPer100g > 0.0) kcalPer100g else (p * 4.0) + (c * 4.0) + (f * 9.0)
        val slug = cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "item" }
        val key = "custom_${slug}_$now"
        val item = FoodItemCacheEntity(
            barcodeOrKey = key,
            name = cleanName,
            brand = "Custom",
            kcalPer100g = resolvedKcal,
            proteinPer100g = p,
            carbsPer100g = c,
            fatPer100g = f,
            servingSizeG = gramsToLog.coerceAtLeast(1.0),
            cachedAtEpochMs = now,
            lastUsedAtEpochMs = now,
            isCustom = 1
        )
        nutritionDao.upsertFoodCache(listOf(item))
        if (alsoLog) {
            logFood(item, gramsToLog.coerceAtLeast(1.0), mealSlot, epochDay)
        }
        return item
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
        saveCustomFood(
            name = name,
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            gramsToLog = gramsToLog,
            mealSlot = mealSlot,
            alsoLog = true
        )
    }

    /**
     * Saves a custom meal directly from total macros & calories (auto-computing kcal from P/C/F if omitted)
     * into food_item_cache (isCustom = 1) and optionally logs it for today.
     */
    suspend fun saveCustomMealTotal(
        name: String,
        totalKcal: Double,
        totalProteinG: Double,
        totalCarbsG: Double,
        totalFatG: Double,
        mealSlot: MealSlot,
        alsoLog: Boolean,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): FoodItemCacheEntity {
        val now = System.currentTimeMillis()
        val cleanName = name.trim().ifBlank { "Custom Meal" }
        val p = totalProteinG.coerceAtLeast(0.0)
        val c = totalCarbsG.coerceAtLeast(0.0)
        val f = totalFatG.coerceAtLeast(0.0)
        val resolvedKcal = if (totalKcal > 0.0) totalKcal else (p * 4.0) + (c * 4.0) + (f * 9.0)
        val slug = cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "meal" }
        val key = "custom_total_${slug}_$now"
        val item = FoodItemCacheEntity(
            barcodeOrKey = key,
            name = cleanName,
            brand = "Custom Meal",
            kcalPer100g = resolvedKcal,
            proteinPer100g = p,
            carbsPer100g = c,
            fatPer100g = f,
            servingSizeG = 100.0,
            cachedAtEpochMs = now,
            lastUsedAtEpochMs = now,
            isCustom = 1
        )
        nutritionDao.upsertFoodCache(listOf(item))
        if (alsoLog) {
            nutritionDao.insertFoodLog(
                FoodLogEntity(
                    dateEpochDay = epochDay,
                    foodKey = key,
                    foodName = cleanName,
                    grams = 100.0,
                    kcal = resolvedKcal.roundToInt(),
                    proteinG = p,
                    carbsG = c,
                    fatG = f,
                    mealSlot = mealSlot.name,
                    loggedAtEpochMs = now
                )
            )
        }
        return item
    }

    suspend fun addCustomMealTotalAndLog(
        name: String,
        totalKcal: Double,
        totalProteinG: Double,
        totalCarbsG: Double,
        totalFatG: Double,
        mealSlot: MealSlot,
        epochDay: Long = LocalDate.now().toEpochDay()
    ) {
        saveCustomMealTotal(
            name = name,
            totalKcal = totalKcal,
            totalProteinG = totalProteinG,
            totalCarbsG = totalCarbsG,
            totalFatG = totalFatG,
            mealSlot = mealSlot,
            alsoLog = true,
            epochDay = epochDay
        )
    }

    suspend fun deleteCustomFood(barcodeOrKey: String) {
        nutritionDao.deleteCachedFood(barcodeOrKey)
    }

    suspend fun updateFoodLog(log: FoodLogEntity) {
        nutritionDao.updateFoodLog(log)
    }

    suspend fun deleteFoodLog(logId: Long) {
        nutritionDao.deleteFoodLog(logId)
    }
}
