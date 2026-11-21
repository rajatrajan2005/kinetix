package com.example.data.repository

import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepository(
    private val userProfileDao: UserProfileDao
) {
    fun observeProfile(): Flow<UserProfileEntity> =
        userProfileDao.observeProfile().map { it ?: UserProfileEntity() }

    suspend fun getProfileOnce(): UserProfileEntity =
        userProfileDao.getProfile() ?: UserProfileEntity().also {
            userProfileDao.upsertProfile(it)
        }

    suspend fun saveProfile(profile: UserProfileEntity) {
        userProfileDao.upsertProfile(profile.copy(id = 1))
    }
}
