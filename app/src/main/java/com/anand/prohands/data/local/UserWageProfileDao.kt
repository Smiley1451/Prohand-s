package com.anand.prohands.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWageProfileDao {

    @Query("SELECT * FROM user_wage_profile WHERE userId = :userId")
    fun getWageProfile(userId: String): Flow<UserWageProfile?>

    @Query("SELECT * FROM user_wage_profile WHERE userId = :userId")
    suspend fun getWageProfileSync(userId: String): UserWageProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserWageProfile)

    @Delete
    suspend fun delete(profile: UserWageProfile)

    @Query("DELETE FROM user_wage_profile WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}

