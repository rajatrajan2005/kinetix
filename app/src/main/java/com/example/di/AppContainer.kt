package com.example.di

import android.content.Context
import com.example.data.local.KinetixDatabase
import com.example.data.remote.OpenFoodFactsClient
import com.example.data.repository.NutritionRepository
import com.example.data.repository.ProgressAndCycleRepository
import com.example.data.repository.StepRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.WaterRepository
import com.example.data.repository.WorkoutRepository
import com.example.media.MediaControlRepository
import com.example.steps.StepSensorManager
import com.example.water.WaterAlarmScheduler

/**
 * Lightweight manual constructor-injection container with lazy singletons
 * to keep cold start under 500 ms and avoid DI framework overhead.
 */
class AppContainer private constructor(appContext: Context) {

    val database: KinetixDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        KinetixDatabase.getInstance(appContext)
    }

    private val openFoodFactsClient: OpenFoodFactsClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenFoodFactsClient()
    }

    val waterAlarmScheduler: WaterAlarmScheduler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WaterAlarmScheduler(appContext)
    }

    val stepSensorManager: StepSensorManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        StepSensorManager(appContext)
    }

    val userProfileRepository: UserProfileRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        UserProfileRepository(database.userProfileDao())
    }

    val workoutRepository: WorkoutRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WorkoutRepository(
            exerciseDao = database.exerciseDao(),
            splitDao = database.splitDao(),
            workoutDao = database.workoutDao()
        )
    }

    val nutritionRepository: NutritionRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NutritionRepository(
            nutritionDao = database.nutritionDao(),
            userProfileDao = database.userProfileDao(),
            offClient = openFoodFactsClient
        )
    }

    val waterRepository: WaterRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WaterRepository(
            waterDao = database.waterDao(),
            userProfileDao = database.userProfileDao(),
            alarmScheduler = waterAlarmScheduler
        )
    }

    val stepRepository: StepRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        StepRepository(database.stepDao())
    }

    val progressAndCycleRepository: ProgressAndCycleRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProgressAndCycleRepository(
            appContext = appContext,
            userProfileDao = database.userProfileDao(),
            progressPhotoDao = database.progressPhotoDao(),
            cycleDao = database.cycleDao()
        )
    }

    val mediaControlRepository: MediaControlRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MediaControlRepository(appContext)
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
        }
    }
}
