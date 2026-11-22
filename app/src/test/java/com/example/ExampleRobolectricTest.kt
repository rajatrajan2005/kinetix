package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KinetixDatabase
import com.example.data.local.entity.CycleLogEntity
import com.example.data.local.entity.ProgressPhotoEntity
import com.example.data.remote.OpenFoodFactsClient
import com.example.data.repository.NutritionRepository
import com.example.data.repository.StepRepository
import com.example.data.repository.WorkoutRepository
import com.example.domain.calculator.CycleCalculator
import com.example.domain.calculator.CyclePhase
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.MealSlot
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.SplitPreset
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var db: KinetixDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.databaseBuilder(
            context,
            KinetixDatabase::class.java,
            "test_kinetix_${System.nanoTime()}.db"
        )
            .createFromAsset("databases/kinetix.db")
            .addMigrations(
                KinetixDatabase.MIGRATION_1_2,
                KinetixDatabase.MIGRATION_2_3,
                KinetixDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kinetix", appName)
    }

    @Test
    fun `prepopulated room asset opens cleanly and filters by muscle head`() = runTest {
        val count = db.exerciseDao().countAll()
        assertTrue("Expected >= 60 pre-populated exercises, got $count", count >= 60)

        val upperChest = db.exerciseDao()
            .observeFiltered(muscleGroup = "Chest", muscleHead = "Upper Chest", query = "")
            .first()
        assertTrue(upperChest.isNotEmpty())
        assertTrue(upperChest.all { it.muscleHead == "Upper Chest" })
    }

    @Test
    fun `step repository accumulates across hardware counter reboot reset`() = runTest {
        val repo = StepRepository(db.stepDao())

        val day = 20500L
        repo.recordHardwareCounter(10_000L, epochDay = day)
        val afterWalk = repo.recordHardwareCounter(12_400L, epochDay = day)
        assertEquals(2_400L, afterWalk.stepsToday)

        // Simulate device reboot: hardware step counter resets to 350
        val afterReboot = repo.recordHardwareCounter(350L, epochDay = day)
        assertEquals(2_750L, afterReboot.stepsToday)
    }

    @Test
    fun `nutrition repository logs and edits direct total macros`() = runTest {
        val repo = NutritionRepository(
            nutritionDao = db.nutritionDao(),
            userProfileDao = db.userProfileDao(),
            offClient = OpenFoodFactsClient()
        )
        val day = 20501L
        repo.addCustomMealTotalAndLog(
            name = "Paneer Bowl",
            totalKcal = 665.0,
            totalProteinG = 42.0,
            totalCarbsG = 40.0,
            totalFatG = 23.0,
            mealSlot = MealSlot.DINNER,
            epochDay = day
        )

        val initial = repo.observeTodayLogs(day).first()
        assertEquals(1, initial.size)
        assertEquals(665, initial.first().kcal)

        repo.updateFoodLog(
            initial.first().copy(foodName = "Paneer Rice Bowl", kcal = 720, proteinG = 45.0)
        )
        val updated = repo.observeTodayLogs(day).first()
        assertEquals("Paneer Rice Bowl", updated.first().foodName)
        assertEquals(720, updated.first().kcal)
        assertEquals(45.0, updated.first().proteinG, 0.01)
    }

    @Test
    fun `workout repository edits split day name and reorders exercises`() = runTest {
        val repo = WorkoutRepository(
            exerciseDao = db.exerciseDao(),
            splitDao = db.splitDao(),
            workoutDao = db.workoutDao()
        )
        repo.ensureStarterSplitIfEmpty()
        val days = repo.observeSplitDays().first()
        val firstDay = days.first()
        repo.updateSplitDay(firstDay.id, "Heavy Upper Push", "Chest, Front Delts")

        val updatedDay = repo.observeSplitDays().first().first { it.id == firstDay.id }
        assertEquals("Heavy Upper Push", updatedDay.name)

        val exercises = db.splitDao().getExercisesForDay(firstDay.id)
        assertTrue(exercises.size >= 2)
        val secondId = exercises[1].id
        repo.moveSplitExercise(firstDay.id, secondId, moveUp = true)
        val reordered = db.splitDao().getExercisesForDay(firstDay.id)
        assertEquals(secondId, reordered[0].id)
    }

    @Test
    fun `cycle calculator and progress photo dao persist cleanly`() = runTest {
        val todayDay = 20510L
        db.cycleDao().upsertCycleLog(
            CycleLogEntity(
                startDateEpochDay = todayDay - 2L,
                flowIntensity = "Medium",
                symptomsCsv = "Fatigue",
                notes = ""
            )
        )
        val logs = db.cycleDao().observeCycleLogs().first()
        val summary = CycleCalculator.summarize(logs, cycleLengthDays = 28, periodLengthDays = 5, todayEpochDay = todayDay)
        assertNotNull(summary)
        assertEquals(3, summary!!.cycleDay)
        assertEquals(CyclePhase.MENSTRUAL, summary.phase)

        val photoId = db.progressPhotoDao().insertPhoto(
            ProgressPhotoEntity(
                weekLabel = "Week 1",
                dateEpochDay = todayDay,
                filePath = "/tmp/w1.jpg",
                weightKgSnapshot = 68.5,
                notes = "Baseline",
                createdAtEpochMs = 1_000L
            )
        )
        val photos = db.progressPhotoDao().observeAllPhotos().first()
        assertEquals(1, photos.size)
        assertEquals(photoId, photos.first().id)
    }

    @Test
    fun `progress and cycle repository saves cropped profile avatar bitmap`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.ProgressAndCycleRepository(
            appContext = context,
            userProfileDao = db.userProfileDao(),
            progressPhotoDao = db.progressPhotoDao(),
            cycleDao = db.cycleDao()
        )
        val bmp = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888)
        val savedPath = repo.saveProfileAvatarBitmap(bmp).getOrThrow()
        assertTrue(java.io.File(savedPath).exists())
        val profile = db.userProfileDao().getProfile()
        assertNotNull(profile)
        assertEquals(savedPath, profile!!.profilePhotoPath)
    }

    @Test
    fun `custom food save-only persists with auto-kcal fallback and supports deletion`() = runTest {
        val repo = NutritionRepository(
            nutritionDao = db.nutritionDao(),
            userProfileDao = db.userProfileDao(),
            offClient = OpenFoodFactsClient()
        )
        val day = 20515L
        val saved = repo.saveCustomMealTotal(
            name = "Oat Shake",
            totalKcal = 0.0,
            totalProteinG = 42.0,
            totalCarbsG = 40.0,
            totalFatG = 23.0,
            mealSlot = MealSlot.BREAKFAST,
            alsoLog = false,
            epochDay = day
        )

        // 42*4 + 40*4 + 23*9 = 168 + 160 + 207 = 535 kcal
        assertEquals(535.0, saved.kcalPer100g, 0.01)
        val customFoods = repo.observeCustomFoods().first()
        assertTrue(customFoods.any { it.barcodeOrKey == saved.barcodeOrKey && it.name == "Oat Shake" })
        // Save Only should not add to today's intake log
        assertTrue(repo.observeTodayLogs(day).first().isEmpty())

        repo.deleteCustomFood(saved.barcodeOrKey)
        assertTrue(repo.observeCustomFoods().first().none { it.barcodeOrKey == saved.barcodeOrKey })
    }

    @Test
    fun `six day double muscle split preset populates exact schedule prescriptions and auto-upgrades untouched splits`() = runTest {
        val repo = WorkoutRepository(
            exerciseDao = db.exerciseDao(),
            splitDao = db.splitDao(),
            workoutDao = db.workoutDao()
        )
        // Simulate untouched legacy 4-day starter split before upgrade
        repo.importSplitFromText(
            """
            Chest + Triceps: Barbell Bench Press
            Back + Biceps: Pull-Up
            Legs: Barbell Back Squat
            Shoulders: Dumbbell Lateral Raise
            """.trimIndent()
        )
        repo.repairSplitAndExerciseTags()

        val days = repo.observeSplitDays().first()
        assertEquals(7, days.size)
        assertEquals("Monday: Chest + Triceps", days[0].name)
        assertEquals("Tuesday: Back + Biceps", days[1].name)
        assertEquals("Wednesday: Shoulders + Abs", days[2].name)
        assertEquals("Thursday: Chest + Triceps", days[3].name)
        assertEquals("Friday: Back + Biceps", days[4].name)
        assertEquals("Saturday: Legs", days[5].name)
        assertEquals("Sunday: Rest", days[6].name)

        // Mon-Sat each have 5 exact exercises, Sunday Rest has 0 exercises
        for (i in 0..5) {
            val exList = db.splitDao().getExercisesForDay(days[i].id)
            assertEquals("Expected 5 exercises on ${days[i].name}", 5, exList.size)
            assertTrue(
                "Zero exercises should fall back to Full Body",
                exList.none { it.muscleGroup == "Full Body" || it.muscleHead == "General" }
            )
        }
        assertTrue(db.splitDao().getExercisesForDay(days[6].id).isEmpty())
        assertTrue(days[6].targetMusclesCsv.contains("Light walking"))

        // Verify per-exercise reps & Compound (150s) vs Isolation (75s) rest times
        val monday = db.splitDao().getExercisesForDay(days[0].id)
        assertEquals("Flat Bench Press", monday[0].exerciseName)
        assertEquals(3, monday[0].targetSets)
        assertEquals(8, monday[0].repMin)
        assertEquals(10, monday[0].repMax)
        assertEquals(WorkoutRepository.COMPOUND_REST_SECONDS, monday[0].restSeconds)

        assertEquals("Pec Deck", monday[2].exerciseName)
        assertEquals(12, monday[2].repMin)
        assertEquals(15, monday[2].repMax)
        assertEquals(WorkoutRepository.ISOLATION_REST_SECONDS, monday[2].restSeconds)

        val tuesday = db.splitDao().getExercisesForDay(days[1].id)
        val row = tuesday.first { it.exerciseName == "Chest-Supported Row" }
        assertEquals("Back", row.muscleGroup)
        assertEquals("Mid-back", row.muscleHead)
        assertEquals(10, row.repMin)
        assertEquals(10, row.repMax)
        assertEquals(WorkoutRepository.COMPOUND_REST_SECONDS, row.restSeconds)

        val wednesday = db.splitDao().getExercisesForDay(days[2].id)
        assertEquals("Shoulder Press", wednesday[0].exerciseName)
        assertEquals(WorkoutRepository.COMPOUND_REST_SECONDS, wednesday[0].restSeconds)
        assertEquals("Cable Crunches", wednesday[3].exerciseName)
        assertEquals(15, wednesday[3].repMin)
        assertEquals(15, wednesday[3].repMax)
        assertEquals(WorkoutRepository.ISOLATION_REST_SECONDS, wednesday[3].restSeconds)

        val saturday = db.splitDao().getExercisesForDay(days[5].id)
        assertEquals("Barbell Squats", saturday[0].exerciseName)
        assertEquals("Standing Calf Raises", saturday[4].exerciseName)
        assertEquals(15, saturday[4].repMax)
        assertEquals(WorkoutRepository.ISOLATION_REST_SECONDS, saturday[4].restSeconds)

        repo.addRestDay()
        val withRest = repo.observeSplitDays().first()
        assertEquals(8, withRest.size)
        assertEquals("Rest day", withRest.last().name)

        repo.ensureSportExercisesSeeded()
        val allExercises = repo.observeAllExercises().first()
        val football = repo.recommendedForSport(SportDiscipline.FOOTBALL, allExercises)
        assertTrue(football.isNotEmpty())
        assertTrue(football.any { it.id == "sport_nordic_curl" })
    }

    @Test
    fun `body recomp goal aligns nutrition protein water steps and carbs`() {
        val targets = TrainingTuningCalculator.recommendedTargetsFor(TrainingGoal.BODY_RECOMP)
        assertEquals(NutritionGoal.RECOMP, targets.nutritionGoal)
        assertEquals(2.1, targets.proteinPerKg, 0.001)
        assertEquals(35.0, targets.waterMlPerKg, 0.001)
        assertEquals(9500, targets.stepGoal)

        val macros = MacroCalculator.calculate(
            age = 26,
            sex = Sex.MALE,
            heightCm = 178.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.MODERATE,
            nutritionGoal = targets.nutritionGoal,
            cutDeficitPct = 0.175,
            proteinPerKg = targets.proteinPerKg
        )
        assertTrue(kotlin.math.abs(macros.targetKcal - (macros.tdeeKcal * 0.97)) <= 2.0)
        assertTrue(macros.carbsG > 0)
        assertTrue(macros.proteinG >= 157)
    }
}
