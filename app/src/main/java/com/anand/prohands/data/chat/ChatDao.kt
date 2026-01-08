package com.anand.prohands.data.chat

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getConversations(): Flow<List<ConversationWithParticipants>>

    @Transaction
    @Query("""
        SELECT c.* FROM conversations c
        INNER JOIN ConversationParticipantCrossRef cp ON c.chatId = cp.chatId
        WHERE cp.userId = :userId
        ORDER BY c.lastMessageTimestamp DESC
    """)
    fun getConversationsForUser(userId: String): Flow<List<ConversationWithParticipants>>

    @Query("SELECT * FROM conversations WHERE chatId = :chatId")
    suspend fun getConversation(chatId: String): ConversationEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND status != 'READ' AND senderId != :currentUserId")
    suspend fun getUnreadMessages(chatId: String, currentUserId: String): List<MessageEntity>

    @Query("SELECT * FROM participants WHERE userId = :userId")
    fun getParticipant(userId: String): Flow<ParticipantEntity?>

    @Query("SELECT * FROM participants WHERE userId = :userId")
    suspend fun getParticipantSync(userId: String): ParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipantCrossRef(crossRef: ConversationParticipantCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    @Query("UPDATE messages SET status = :status WHERE chatId = :chatId AND status != 'READ' AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(chatId: String, status: MessageStatus = MessageStatus.READ, currentUserId: String)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp, unreadCounts = :unreadCounts WHERE chatId = :chatId")
    suspend fun updateConversationSnippet(chatId: String, lastMessage: String, timestamp: String, unreadCounts: Map<String, Int>)

    @Query("UPDATE conversations SET unreadCounts = :unreadCounts WHERE chatId = :chatId")
    suspend fun updateUnreadCounts(chatId: String, unreadCounts: Map<String, Int>)
    
    @Query("UPDATE participants SET isOnline = :isOnline, lastSeen = :lastSeen WHERE userId = :userId")
    suspend fun updateUserPresence(userId: String, isOnline: Boolean, lastSeen: Long?)

    @Transaction
    suspend fun insertConversationWithParticipants(conversation: ConversationEntity, participants: List<ParticipantEntity>) {
        insertConversation(conversation)
        participants.forEach { participant ->
            insertParticipant(participant)
            insertParticipantCrossRef(ConversationParticipantCrossRef(conversation.chatId, participant.userId))
        }
    }


    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM participants")
    suspend fun deleteAllParticipants()

    @Query("DELETE FROM ConversationParticipantCrossRef")
    suspend fun deleteAllCrossRefs()

    @Transaction
    suspend fun clearAllChatData() {
        deleteAllCrossRefs()
        deleteAllMessages()
        deleteAllParticipants()
        deleteAllConversations()
    }
}
