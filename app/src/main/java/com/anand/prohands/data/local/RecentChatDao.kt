package com.anand.prohands.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChatDao {
    @Query("SELECT * FROM recent_chats ORDER BY timestamp DESC")
    fun getAllRecentChats(): Flow<List<RecentChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recentChat: RecentChatEntity)

    @Query("UPDATE recent_chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearUnreadCount(chatId: String)
    
    @Query("SELECT * FROM recent_chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getRecentChat(chatId: String): RecentChatEntity?
}
