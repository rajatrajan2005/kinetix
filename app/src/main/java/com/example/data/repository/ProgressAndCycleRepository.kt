package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.dao.CycleDao
import com.example.data.local.dao.ProgressPhotoDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.CycleLogEntity
import com.example.data.local.entity.ProgressPhotoEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class ProgressAndCycleRepository(
    private val appContext: Context,
    private val userProfileDao: UserProfileDao,
    private val progressPhotoDao: ProgressPhotoDao,
    private val cycleDao: CycleDao
) {
    fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>> =
        progressPhotoDao.observeAllPhotos()

    fun observeCycleLogs(): Flow<List<CycleLogEntity>> =
        cycleDao.observeCycleLogs()

    suspend fun saveProfileAvatar(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(appContext.filesDir, "profile").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val dest = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to read selected image")

            val current = userProfileDao.getProfile() ?: UserProfileEntity()
            userProfileDao.upsertProfile(current.copy(id = 1, profilePhotoPath = dest.absolutePath))
            dest.absolutePath
        }
    }

    suspend fun saveProfileAvatarBitmap(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(appContext.filesDir, "profile").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val dest = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            dest.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            val current = userProfileDao.getProfile() ?: UserProfileEntity()
            userProfileDao.upsertProfile(current.copy(id = 1, profilePhotoPath = dest.absolutePath))
            dest.absolutePath
        }
    }

    suspend fun addProgressPhoto(
        uri: Uri,
        weekLabel: String,
        weightKgSnapshot: Double,
        notes: String,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(appContext.filesDir, "progress").apply { mkdirs() }
            val now = System.currentTimeMillis()
            val dest = File(dir, "progress_${epochDay}_$now.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to read selected image")

            progressPhotoDao.insertPhoto(
                ProgressPhotoEntity(
                    weekLabel = weekLabel.trim().ifBlank { "Week Check-In" },
                    dateEpochDay = epochDay,
                    filePath = dest.absolutePath,
                    weightKgSnapshot = weightKgSnapshot,
                    notes = notes.trim(),
                    createdAtEpochMs = now
                )
            )
        }
    }

    suspend fun deleteProgressPhoto(photo: ProgressPhotoEntity) = withContext(Dispatchers.IO) {
        progressPhotoDao.deletePhoto(photo.id)
        runCatching { File(photo.filePath).delete() }
    }

    suspend fun logCycle(
        startDateEpochDay: Long,
        endDateEpochDay: Long? = null,
        flowIntensity: String,
        symptomsCsv: String,
        notes: String
    ): Long {
        return cycleDao.upsertCycleLog(
            CycleLogEntity(
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                flowIntensity = flowIntensity,
                symptomsCsv = symptomsCsv,
                notes = notes.trim()
            )
        )
    }

    suspend fun deleteCycleLog(id: Long) {
        cycleDao.deleteCycleLog(id)
    }
}
