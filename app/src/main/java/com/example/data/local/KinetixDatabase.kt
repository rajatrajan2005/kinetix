package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CycleDao
import com.example.data.local.dao.ExerciseDao
import com.example.data.local.dao.NutritionDao
import com.example.data.local.dao.ProgressPhotoDao
import com.example.data.local.dao.SplitDao
import com.example.data.local.dao.StepDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WaterDao
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.CycleLogEntity
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.data.local.entity.ProgressPhotoEntity
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
        StepRecordEntity::class,
        ProgressPhotoEntity::class,
        CycleLogEntity::class
    ],
    version = 3,
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
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun cycleDao(): CycleDao

    companion object {
        private const val DB_NAME = "kinetix.db"
        private const val ASSET_DB_PATH = "databases/kinetix.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `displayName` TEXT NOT NULL DEFAULT 'Athlete'")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `profilePhotoPath` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `trackCycle` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `cycleLengthDays` INTEGER NOT NULL DEFAULT 28")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `periodLengthDays` INTEGER NOT NULL DEFAULT 5")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `progress_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekLabel` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `weightKgSnapshot` REAL NOT NULL, `notes` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_photos_dateEpochDay` ON `progress_photos` (`dateEpochDay`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cycle_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startDateEpochDay` INTEGER NOT NULL, `endDateEpochDay` INTEGER, `flowIntensity` TEXT NOT NULL, `symptomsCsv` TEXT NOT NULL, `notes` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cycle_logs_startDateEpochDay` ON `cycle_logs` (`startDateEpochDay`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `sportType` TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
