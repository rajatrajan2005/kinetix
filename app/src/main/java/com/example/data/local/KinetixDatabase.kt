package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ExerciseDao
import com.example.data.local.dao.NutritionDao
import com.example.data.local.dao.SplitDao
import com.example.data.local.dao.StepDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WaterDao
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.SplitExerciseEntity
import com.example.data.local.entity.StepRecordEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WaterLogEntity
import com.example.data.local.entity.WaterScheduleStateEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        UserProfileEntity::class,
        SplitDayEntity::class,
        SplitExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        FoodItemCacheEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class,
        WaterScheduleStateEntity::class,
        StepRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KinetixDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun splitDao(): SplitDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun waterDao(): WaterDao
    abstract fun stepDao(): StepDao

    companion object {
        private const val DB_NAME = "kinetix.db"
        private const val ASSET_DB_PATH = "databases/kinetix.db"

        @Volatile
        private var instance: KinetixDatabase? = null

        fun getInstance(context: Context): KinetixDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(appContext: Context): KinetixDatabase {
            val builder = Room.databaseBuilder(
                appContext,
                KinetixDatabase::class.java,
                DB_NAME
            )
            val hasAsset = runCatching {
                appContext.assets.open(ASSET_DB_PATH).use { true }
            }.getOrDefault(false)
            if (hasAsset) {
                builder.createFromAsset(ASSET_DB_PATH)
            }
            return builder.build()
        }
    }
}
