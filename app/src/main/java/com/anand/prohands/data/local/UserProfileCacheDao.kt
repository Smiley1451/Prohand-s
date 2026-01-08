package com.anand.prohands.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "user_profiles_cache")
data class UserProfileCache(
    @PrimaryKey val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long? = null
)

@Dao
interface UserProfileCacheDao {

    @Query("SELECT * FROM user_profiles_cache WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileCache?

    @Query("SELECT * FROM user_profiles_cache WHERE userId = :userId")
    fun getUserProfileFlow(userId: String): Flow<UserProfileCache?>

    @Query("SELECT * FROM user_profiles_cache WHERE userId IN (:userIds)")
    suspend fun getUserProfiles(userIds: List<String>): List<UserProfileCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileCache)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(userProfiles: List<UserProfileCache>)

    @Query("DELETE FROM user_profiles_cache WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)

    @Query("DELETE FROM user_profiles_cache WHERE cachedAt < :expireBefore")
    suspend fun deleteExpiredProfiles(expireBefore: Long)

    @Query("SELECT COUNT(*) FROM user_profiles_cache")
    suspend fun getCacheSize(): Int

    @Query("DELETE FROM user_profiles_cache WHERE cachedAt IN (SELECT cachedAt FROM user_profiles_cache ORDER BY cachedAt ASC LIMIT :count)")
    suspend fun deleteOldestProfiles(count: Int)
}
