package com.anand.prohands.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_chats")
data class RecentChatEntity(
    @PrimaryKey
    val chatId: String,
    val recipientId: String,
    val recipientName: String?,
    val profilePictureUrl: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0
)
