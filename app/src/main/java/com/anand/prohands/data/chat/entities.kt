package com.anand.prohands.data.chat

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverters
import com.anand.prohands.data.chat.converters.MapConverter

@Entity(tableName = "conversations")
@TypeConverters(MapConverter::class)
data class ConversationEntity(
    @PrimaryKey val chatId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: String?, // ISO 8601 String
    val unreadCounts: Map<String, Int> = emptyMap(),
    val participants: List<String> = emptyList(),
    val updatedAt: String // ISO 8601 String
)

@Entity(tableName = "messages")
@TypeConverters(MapConverter::class)
data class MessageEntity(
    @PrimaryKey val messageId: String, // Renamed from 'id' to match JS/DTO
    val chatId: String,
    val senderId: String,
    val recipientId: String, // Added to match logic
    val content: String,
    val timestamp: String, // ISO 8601 String
    val status: MessageStatus,
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String>? = null
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val profilePictureUrl: String?,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis() // Timestamp for cache invalidation
)

data class ConversationWithParticipants(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "userId",
        associateBy = Junction(ConversationParticipantCrossRef::class)
    )
    val participants: List<ParticipantEntity>
)

@Entity(primaryKeys = ["chatId", "userId"])
data class ConversationParticipantCrossRef(
    val chatId: String,
    val userId: String
)

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED, DELETED
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE, SYSTEM
}
