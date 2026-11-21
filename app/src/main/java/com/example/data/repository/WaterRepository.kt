package com.example.data.repository

import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WaterDao
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WaterLogEntity
import com.example.data.local.entity.WaterScheduleStateEntity
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.WaterDoseSlot
import com.example.water.WaterAlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

class WaterRepository(
    private val waterDao: WaterDao,
    private val userProfileDao: UserProfileDao,
    private val alarmScheduler: WaterAlarmScheduler
) {
    fun observeTodayLogs(epochDay: Long = LocalDate.now().toEpochDay()): Flow<List<WaterLogEntity>> =
        waterDao.observeWaterLogsForDay(epochDay)

    fun observeTodayTotalMl(epochDay: Long = LocalDate.now().toEpochDay()): Flow<Int> =
        waterDao.observeTotalWaterForDay(epochDay)

    fun observeTodaySchedule(epochDay: Long = LocalDate.now().toEpochDay()): Flow<WaterScheduleStateEntity?> =
        waterDao.observeScheduleState(epochDay)

    suspend fun getScheduleStateOnce(epochDay: Long): WaterScheduleStateEntity? =
        waterDao.getScheduleStateOnce(epochDay)

    suspend fun onFirstAppOpenToday() {
        val today = LocalDate.now().toEpochDay()
        val existing = waterDao.getScheduleStateOnce(today)
        if (existing == null) {
            val profile = userProfileDao.getProfile() ?: UserProfileEntity()
            val wakeMins = if (profile.manualWakeOverride == 1) {
                profile.wakeTimeMinutes
            } else {
                val now = LocalTime.now()
                (now.hour * 60) + now.minute
            }
            saveAndSchedule(
                epochDay = today,
                wakeMinutes = wakeMins,
                bedMinutes = profile.bedTimeMinutes,
                targetMl = WaterScheduleCalculator.dailyTargetMl(profile.weightKg, profile.waterMlPerKg),
                isConfirmedAwake = 1
            )
        }
    }

    suspend fun markWakeUpNow(manualOverride: Boolean = false) {
        val now = LocalTime.now()
        val mins = (now.hour * 60) + now.minute
        setWakeTimeMinutes(mins, manualOverride)
    }

    suspend fun setWakeTimeMinutes(wakeMinutes: Int, manualOverride: Boolean = true) {
        val profile = userProfileDao.getProfile() ?: UserProfileEntity()
        val updatedProfile = profile.copy(
            wakeTimeMinutes = wakeMinutes.coerceIn(0, 23 * 60 + 59),
            manualWakeOverride = if (manualOverride) 1 else 0
        )
        userProfileDao.upsertProfile(updatedProfile)
        saveAndSchedule(
            epochDay = LocalDate.now().toEpochDay(),
            wakeMinutes = updatedProfile.wakeTimeMinutes,
            bedMinutes = updatedProfile.bedTimeMinutes,
            targetMl = WaterScheduleCalculator.dailyTargetMl(
                updatedProfile.weightKg,
                updatedProfile.waterMlPerKg
            ),
            isConfirmedAwake = 1
        )
    }

    suspend fun logWater(amountMl: Int, source: String = "manual") {
        val today = LocalDate.now().toEpochDay()
        waterDao.insertWaterLog(
            WaterLogEntity(
                dateEpochDay = today,
                amountMl = amountMl.coerceIn(25, 2000),
                loggedAtEpochMs = System.currentTimeMillis(),
                source = source
            )
        )
        rescheduleFromCurrentState()
    }

    suspend fun deleteWaterLog(id: Long) {
        waterDao.deleteWaterLog(id)
        rescheduleFromCurrentState()
    }

    suspend fun snoozeMinutes(minutes: Int, amountMl: Int) {
        val today = LocalDate.now().toEpochDay()
        val existing = waterDao.getScheduleStateOnce(today)
        val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
        if (existing != null) {
            waterDao.upsertScheduleState(
                existing.copy(
                    nextReminderAtEpochMs = snoozeUntil,
                    nextReminderAmountMl = amountMl,
                    snoozedUntilEpochMs = snoozeUntil
                )
            )
        }
        alarmScheduler.scheduleReminder(snoozeUntil, amountMl)
    }

    suspend fun computeTodaySlots(): List<WaterDoseSlot> {
        val todayDate = LocalDate.now()
        val today = todayDate.toEpochDay()
        val profile = userProfileDao.getProfile() ?: UserProfileEntity()
        val state = waterDao.getScheduleStateOnce(today)
        val wakeMins = state?.wakeMinutes ?: profile.wakeTimeMinutes
        val bedMins = state?.bedMinutes ?: profile.bedTimeMinutes
        val target = state?.targetMl ?: WaterScheduleCalculator.dailyTargetMl(
            profile.weightKg,
            profile.waterMlPerKg
        )
        val consumed = waterDao.getTotalWaterForDayOnce(today)

        return WaterScheduleCalculator.buildRemainingSchedule(
            date = todayDate,
            wakeTime = LocalTime.of(wakeMins / 60, wakeMins % 60),
            bedTime = LocalTime.of(bedMins / 60, bedMins % 60),
            nowEpochMs = System.currentTimeMillis(),
            targetMl = target,
            consumedMl = consumed
        )
    }

    suspend fun rescheduleFromCurrentState() {
        val todayDate = LocalDate.now()
        val today = todayDate.toEpochDay()
        val profile = userProfileDao.getProfile() ?: UserProfileEntity()
        val state = waterDao.getScheduleStateOnce(today)
        val wakeMins = state?.wakeMinutes ?: profile.wakeTimeMinutes
        val bedMins = state?.bedMinutes ?: profile.bedTimeMinutes
        val targetMl = WaterScheduleCalculator.dailyTargetMl(profile.weightKg, profile.waterMlPerKg)

        saveAndSchedule(
            epochDay = today,
            wakeMinutes = wakeMins,
            bedMinutes = bedMins,
            targetMl = targetMl,
            isConfirmedAwake = state?.isConfirmedAwake ?: 1
        )
    }

    private suspend fun saveAndSchedule(
        epochDay: Long,
        wakeMinutes: Int,
        bedMinutes: Int,
        targetMl: Int,
        isConfirmedAwake: Int
    ) {
        val consumed = waterDao.getTotalWaterForDayOnce(epochDay)
        val slots = WaterScheduleCalculator.buildRemainingSchedule(
            date = LocalDate.ofEpochDay(epochDay),
            wakeTime = LocalTime.of(wakeMinutes / 60, wakeMinutes % 60),
            bedTime = LocalTime.of(bedMinutes / 60, bedMinutes % 60),
            nowEpochMs = System.currentTimeMillis(),
            targetMl = targetMl,
            consumedMl = consumed
        )
        val next = slots.firstOrNull()
        waterDao.upsertScheduleState(
            WaterScheduleStateEntity(
                dateEpochDay = epochDay,
                wakeMinutes = wakeMinutes,
                bedMinutes = bedMinutes,
                targetMl = targetMl,
                nextReminderAtEpochMs = next?.triggerEpochMs,
                nextReminderAmountMl = next?.amountMl ?: 0,
                snoozedUntilEpochMs = null,
                isConfirmedAwake = isConfirmedAwake
            )
        )
        if (next != null) {
            alarmScheduler.scheduleReminder(next.triggerEpochMs, next.amountMl)
        } else {
            alarmScheduler.cancelReminder()
        }
    }
}
