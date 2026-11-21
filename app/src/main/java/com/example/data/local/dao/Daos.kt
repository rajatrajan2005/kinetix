package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup ASC")
    fun observeMuscleGroups(): Flow<List<String>>

    @Query("SELECT DISTINCT muscleHead FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY muscleHead ASC")
    fun observeHeadsForMuscle(muscleGroup: String): Flow<List<String>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:muscleGroup = '' OR muscleGroup = :muscleGroup)
          AND (:muscleHead = '' OR muscleHead = :muscleHead)
          AND (:query = '' OR name LIKE '%' || :query || '%' OR muscleHead LIKE '%' || :query || '%' OR equipment LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun observeFiltered(
        muscleGroup: String,
        muscleHead: String,
        query: String
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE LOWER(name) LIKE '%' || LOWER(:nameQuery) || '%' LIMIT 1")
    suspend fun findFirstByName(nameQuery: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)
}

@Dao
interface SplitDao {
    @Query("SELECT * FROM split_days ORDER BY dayOrder ASC")
    fun observeSplitDays(): Flow<List<SplitDayEntity>>

    @Query("SELECT * FROM split_days ORDER BY dayOrder ASC")
    suspend fun getSplitDaysOnce(): List<SplitDayEntity>

    @Query("SELECT * FROM split_exercises ORDER BY splitDayId ASC, sortOrder ASC")
    fun observeAllSplitExercises(): Flow<List<SplitExerciseEntity>>

    @Query("SELECT * FROM split_exercises WHERE splitDayId = :dayId ORDER BY sortOrder ASC")
    suspend fun getExercisesForDay(dayId: Long): List<SplitExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: SplitDayEntity): Long

    @Update
    suspend fun updateDay(day: SplitDayEntity)

    @Query("DELETE FROM split_days WHERE id = :dayId")
    suspend fun deleteDay(dayId: Long)

    @Query("DELETE FROM split_days")
    suspend fun clearAllDays()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitExercise(exercise: SplitExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitExercises(exercises: List<SplitExerciseEntity>)

    @Query("DELETE FROM split_exercises WHERE id = :id")
    suspend fun deleteSplitExercise(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM split_exercises WHERE splitDayId = :dayId")
    suspend fun nextSortOrder(dayId: Long): Int

    @Transaction
    suspend fun replaceEntireSplit(daysWithExercises: List<Pair<SplitDayEntity, List<SplitExerciseEntity>>>) {
        clearAllDays()
        daysWithExercises.forEach { (day, exercises) ->
            val newDayId = insertDay(day.copy(id = 0))
            val mapped = exercises.mapIndexed { idx, ex ->
                ex.copy(id = 0, splitDayId = newDayId, sortOrder = idx)
            }
            if (mapped.isNotEmpty()) insertSplitExercises(mapped)
        }
    }
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions WHERE isActive = 1 ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE isActive = 1 ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE isActive = 0 ORDER BY startedAtEpochMs DESC LIMIT 30")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY completedAtEpochMs ASC, setNumber ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY completedAtEpochMs ASC, setNumber ASC")
    suspend fun getSetsForSessionOnce(sessionId: Long): List<WorkoutSetEntity>

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.isActive = 0 AND ws.exerciseId = :exerciseId
        ORDER BY ws.completedAtEpochMs DESC
        LIMIT 20
        """
    )
    suspend fun getRecentCompletedSetsForExercise(exerciseId: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.isActive = 0 AND ws.sessionId = (
            SELECT s2.id FROM workout_sessions s2
            INNER JOIN workout_sets ws2 ON ws2.sessionId = s2.id
            WHERE s2.isActive = 0 AND ws2.exerciseId = :exerciseId
            ORDER BY s2.startedAtEpochMs DESC
            LIMIT 1
        ) AND ws.exerciseId = :exerciseId
        ORDER BY ws.setNumber ASC
        """
    )
    suspend fun getLastSessionSetsForExercise(exerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE completedAtEpochMs >= :sinceEpochMs ORDER BY completedAtEpochMs DESC")
    fun observeSetsSince(sinceEpochMs: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId IN (SELECT id FROM workout_sessions WHERE isActive = 0 ORDER BY startedAtEpochMs DESC LIMIT 30)")
    fun observeRecentHistorySets(): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("UPDATE workout_sessions SET isActive = 0, completedAtEpochMs = :completedAtEpochMs WHERE id = :sessionId")
    suspend fun finishSession(sessionId: Long, completedAtEpochMs: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM food_logs WHERE dateEpochDay = :epochDay ORDER BY loggedAtEpochMs DESC")
    fun observeFoodLogsForDay(epochDay: Long): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLogEntity): Long

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Long)

    @Query(
        """
        SELECT * FROM food_item_cache
        WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'
        ORDER BY lastUsedAtEpochMs DESC, cachedAtEpochMs DESC
        LIMIT 40
        """
    )
    fun observeCachedSearch(query: String): Flow<List<FoodItemCacheEntity>>

    @Query("SELECT * FROM food_item_cache WHERE lastUsedAtEpochMs > 0 OR isCustom = 1 ORDER BY lastUsedAtEpochMs DESC, cachedAtEpochMs DESC LIMIT 15")
    fun observeRecentFoods(): Flow<List<FoodItemCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodCache(items: List<FoodItemCacheEntity>)

    @Query("UPDATE food_item_cache SET lastUsedAtEpochMs = :usedAtEpochMs WHERE barcodeOrKey = :key")
    suspend fun markFoodUsed(key: String, usedAtEpochMs: Long)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE dateEpochDay = :epochDay ORDER BY loggedAtEpochMs DESC")
    fun observeWaterLogsForDay(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE dateEpochDay = :epochDay")
    fun observeTotalWaterForDay(epochDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE dateEpochDay = :epochDay")
    suspend fun getTotalWaterForDayOnce(epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLog(id: Long)

    @Query("SELECT * FROM water_schedule_state WHERE dateEpochDay = :epochDay LIMIT 1")
    fun observeScheduleState(epochDay: Long): Flow<WaterScheduleStateEntity?>

    @Query("SELECT * FROM water_schedule_state WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getScheduleStateOnce(epochDay: Long): WaterScheduleStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleState(state: WaterScheduleStateEntity)
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_records WHERE dateEpochDay = :epochDay LIMIT 1")
    fun observeStepRecord(epochDay: Long): Flow<StepRecordEntity?>

    @Query("SELECT * FROM step_records WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getStepRecordOnce(epochDay: Long): StepRecordEntity?

    @Query("SELECT * FROM step_records ORDER BY dateEpochDay DESC LIMIT 14")
    fun observeRecentSteps(): Flow<List<StepRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepRecord(record: StepRecordEntity)
}
