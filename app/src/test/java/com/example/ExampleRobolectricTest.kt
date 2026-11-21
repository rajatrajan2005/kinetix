package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KinetixDatabase
import com.example.data.repository.StepRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `nutrition repository logs direct total macros without per 100g conversion`() = runTest {
        val repo = com.example.data.repository.NutritionRepository(
            nutritionDao = db.nutritionDao(),
            userProfileDao = db.userProfileDao(),
            offClient = com.example.data.remote.OpenFoodFactsClient()
        )
        val day = 20501L
        repo.addCustomMealTotalAndLog(
            name = "Paneer Bowl",
            totalKcal = 665.0,
            totalProteinG = 42.0,
            totalCarbsG = 40.0,
            totalFatG = 23.0,
            mealSlot = com.example.domain.model.MealSlot.DINNER,
            epochDay = day
        )

        val logs = repo.observeTodayLogs(day).first()
        assertEquals(1, logs.size)
        assertEquals(665, logs.first().kcal)
        assertEquals(42.0, logs.first().proteinG, 0.01)
        assertEquals(23.0, logs.first().fatG, 0.01)
    }
}
