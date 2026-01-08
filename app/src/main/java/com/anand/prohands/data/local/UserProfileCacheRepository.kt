package com.anand.prohands.data.local

import android.util.Log
import com.anand.prohands.data.ClientProfileDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class UserProfileCacheRepository(
    private val userProfileCacheDao: UserProfileCacheDao
) {
    companion object {
        private const val TAG = "UserProfileCache"
        private const val CACHE_EXPIRY_HOURS = 24L // 24 hours
        private const val MAX_CACHE_SIZE = 500
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    suspend fun getCachedUserProfile(userId: String): UserProfileCache? {
        return try {
            userProfileCacheDao.getUserProfile(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached profile for user $userId", e)
            null
        }
    }


    fun getUserProfileFlow(userId: String): Flow<UserProfileCache?> {
        return userProfileCacheDao.getUserProfileFlow(userId)
    }


    suspend fun cacheUserProfile(userProfile: ClientProfileDto) {
        try {
            val cached = UserProfileCache(
                userId = userProfile.userId,
                username = userProfile.name,
                profilePictureUrl = userProfile.profilePictureUrl,
                isOnline = false, // This would need to come from real-time status
                lastSeen = null,
                cachedAt = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )

            userProfileCacheDao.insertUserProfile(cached)
            Log.d(TAG, "Cached profile for user: ${userProfile.userId}")


            cleanupCacheIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching profile for user ${userProfile.userId}", e)
        }
    }


    suspend fun cacheUserProfiles(userProfiles: List<ClientProfileDto>) {
        try {
            val cached = userProfiles.map { profile ->
                UserProfileCache(
                    userId = profile.userId,
                    username = profile.name,
                    profilePictureUrl = profile.profilePictureUrl,
                    isOnline = false,
                    lastSeen = null,
                    cachedAt = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )
            }

            userProfileCacheDao.insertUserProfiles(cached)
            Log.d(TAG, "Cached ${cached.size} profiles")

            cleanupCacheIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching multiple profiles", e)
        }
    }


    suspend fun updateCachedProfileIfChanged(userProfile: ClientProfileDto): Boolean {
        try {
            val existing = getCachedUserProfile(userProfile.userId)

            val hasChanges = existing == null ||
                existing.username != userProfile.name ||
                existing.profilePictureUrl != userProfile.profilePictureUrl

            if (hasChanges) {
                cacheUserProfile(userProfile)
                Log.d(TAG, "Updated cached profile for user: ${userProfile.userId}")
                return true
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cached profile for user ${userProfile.userId}", e)
            return false
        }
    }


    suspend fun getCachedUserProfiles(userIds: List<String>): Map<String, UserProfileCache> {
        return try {
            userProfileCacheDao.getUserProfiles(userIds)
                .associateBy { it.userId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached profiles for users: $userIds", e)
            emptyMap()
        }
    }


    suspend fun isProfileCacheValid(userId: String): Boolean {
        return try {
            val profile = getCachedUserProfile(userId)
            if (profile == null) return false

            val expiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000L // Convert to milliseconds
            val isExpired = (System.currentTimeMillis() - profile.cachedAt) > expiryTime

            !isExpired
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cache validity for user $userId", e)
            false
        }
    }

    private suspend fun cleanupCacheIfNeeded() {
        try {
            // Remove expired entries
            val expiryTime = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            userProfileCacheDao.deleteExpiredProfiles(expiryTime)

            // Limit cache size
            val cacheSize = userProfileCacheDao.getCacheSize()
            if (cacheSize > MAX_CACHE_SIZE) {
                val deleteCount = cacheSize - MAX_CACHE_SIZE + 50 // Delete extra 50 to avoid frequent cleanup
                userProfileCacheDao.deleteOldestProfiles(deleteCount)
                Log.d(TAG, "Cleaned up $deleteCount old cache entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
        }
    }


    suspend fun clearAllCache() {
        try {
            val expiryTime = System.currentTimeMillis() + 1000L // Future time to delete all
            userProfileCacheDao.deleteExpiredProfiles(expiryTime)
            Log.d(TAG, "Cleared all cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    /**
     * Preload profiles for a list of users (fire and forget).
     */
    fun preloadProfiles(userIds: List<String>, fetchFromNetwork: suspend (String) -> ClientProfileDto?) {
        coroutineScope.launch {
            try {
                userIds.forEach { userId ->
                    if (!isProfileCacheValid(userId)) {
                        val profile = fetchFromNetwork(userId)
                        profile?.let { cacheUserProfile(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading profiles", e)
            }
        }
    }
}
