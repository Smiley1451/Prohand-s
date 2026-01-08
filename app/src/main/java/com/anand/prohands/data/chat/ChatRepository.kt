package com.anand.prohands.data.chat

import android.util.Log
import com.anand.prohands.network.ChatService
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.EOFException
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository(
    private val chatDao: ChatDao, 
    private val chatService: ChatService
) {

    var currentUserId: String? = null
    private var lastKnownUserId: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val profileApi: ProfileApi by lazy {
        RetrofitClient.instance.create(ProfileApi::class.java)
    }

    init {
        repositoryScope.launch {
            WebSocketClient.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }
    }

    fun getConversations(): Flow<List<ConversationWithParticipants>> = chatDao.getConversations()


    fun getConversationsForUser(userId: String): Flow<List<ConversationWithParticipants>> =
        chatDao.getConversationsForUser(userId)

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = chatDao.getMessages(chatId)

    fun getParticipant(userId: String): Flow<ParticipantEntity?> = chatDao.getParticipant(userId)


    suspend fun clearAllDataForUserSwitch() {
        Log.d("ChatRepository", "Clearing all chat data for user switch")
        chatDao.clearAllChatData()
    }


    suspend fun initializeForUser(userId: String) {
        if (lastKnownUserId != null && lastKnownUserId != userId) {
            Log.d("ChatRepository", "User changed from $lastKnownUserId to $userId, clearing old data")
            clearAllDataForUserSwitch()
        }

        currentUserId = userId
        lastKnownUserId = userId

        fetchConversations(userId)
    }


    suspend fun fetchAndCacheParticipant(userId: String): ParticipantEntity? {
        return try {
            val response = profileApi.getProfile(userId)
            if (response.isSuccessful) {
                val profile = response.body()
                if (profile != null) {
                    val existing = chatDao.getParticipantSync(userId)
                    val participant = ParticipantEntity(
                        userId = profile.userId,
                        name = profile.name ?: "User",
                        profilePictureUrl = profile.profilePictureUrl,
                        isOnline = existing?.isOnline ?: false,
                        lastSeen = existing?.lastSeen,
                        lastUpdated = System.currentTimeMillis()
                    )
                    chatDao.insertParticipant(participant)
                    Log.d("ChatRepository", "Cached participant: ${participant.name} (${participant.userId})")
                    participant
                } else null
            } else {
                Log.e("ChatRepository", "Failed to fetch profile for $userId: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching participant $userId", e)
            null
        }
    }


    suspend fun getOrFetchParticipant(userId: String, maxAgeMs: Long = 3600000L): ParticipantEntity? {
        val cached = chatDao.getParticipantSync(userId)

        if (cached == null) {
            return fetchAndCacheParticipant(userId)
        }

        if (cached.name == "User" || cached.profilePictureUrl.isNullOrEmpty()) {
            repositoryScope.launch {
                fetchAndCacheParticipant(userId)
            }
        }

        val age = System.currentTimeMillis() - cached.lastUpdated
        if (age > maxAgeMs) {
            repositoryScope.launch {
                fetchAndCacheParticipant(userId)
            }
        }

        return cached
    }


    fun refreshParticipantIfStale(userId: String, maxAgeMs: Long = 3600000L) {
        repositoryScope.launch {
            val cached = chatDao.getParticipantSync(userId)
            if (cached == null || (System.currentTimeMillis() - cached.lastUpdated) > maxAgeMs) {
                fetchAndCacheParticipant(userId)
            }
        }
    }


    suspend fun ensureParticipantProfiles(userId: String) {
        try {
            val conversations = chatDao.getConversationsForUser(userId).firstOrNull() ?: return

            for (conv in conversations) {
                for (participant in conv.participants) {
                    if (participant.userId == userId) continue

                    if (participant.name == "User" || participant.profilePictureUrl.isNullOrEmpty()) {
                        Log.d("ChatRepository", "Fetching missing profile for ${participant.userId}")
                        fetchAndCacheParticipant(participant.userId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error ensuring participant profiles", e)
        }
    }


    suspend fun getParticipantSync(userId: String): ParticipantEntity? {
        return chatDao.getParticipantSync(userId)
    }



    suspend fun syncData() {
        val lastTimestamp = chatDao.getConversations().firstOrNull()?.firstOrNull()?.conversation?.lastMessageTimestamp
        val since = lastTimestamp ?: "1970-01-01T00:00:00Z"
        
        try {
            val response = chatService.sync(since)
            if (response.isSuccessful) {
                val syncData = response.body()
                syncData?.messages?.forEach { messageDto ->
                    chatDao.insertMessage(messageDto.toEntity())
                }
                syncData?.statusUpdates?.forEach { statusUpdate ->
                    chatDao.updateMessageStatus(statusUpdate.messageId, statusUpdate.status)
                }
            }
        } catch (e: EOFException) {
            Log.d("ChatRepository", "Sync returned empty body, treating as success")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Sync failed", e)
        }
    }

    suspend fun loadHistory(chatId: String) {
        try {
            val response = chatService.getHistory(chatId, 0, 50)
            if (response.isSuccessful) {
                response.body()?.forEach { messageDto ->
                    chatDao.insertMessage(messageDto.toEntity())
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Load history failed", e)
        }
    }


    suspend fun loadHistoryPage(chatId: String, page: Int, size: Int): Int {
        return try {
            val response = chatService.getHistory(chatId, page, size)
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                messages.forEach { messageDto ->
                    chatDao.insertMessage(messageDto.toEntity())
                }
                messages.size
            } else {
                Log.e("ChatRepository", "Load history page failed: ${response.code()}")
                0
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Load history page failed", e)
            0
        }
    }

    suspend fun sendMessage(recipientId: String, content: String, type: MessageType = MessageType.TEXT) {
        val sanitized = content.trim()
        if (sanitized.isEmpty()) return

        val sender = currentUserId ?: WebSocketClient.myUserId ?: return

        val messageDto = WebSocketClient.sendMessage(recipientId, sanitized, type)
        if (messageDto != null) {
            chatDao.insertMessage(messageDto.toEntity())
            updateLocalConversation(messageDto)
        } else {
            try {
                val localId = "local-${UUID.randomUUID().toString().take(8)}"
                val chatId = WebSocketClient.generateChatId(sender, recipientId)
                val pendingEntity = MessageEntity(
                    messageId = localId,
                    chatId = chatId,
                    senderId = sender,
                    recipientId = recipientId,
                    content = sanitized,
                    timestamp = isoNow(),
                    status = MessageStatus.PENDING,
                    type = type,
                    metadata = mapOf("local" to "true")
                )
                chatDao.insertMessage(pendingEntity)
                chatDao.updateConversationSnippet(chatId, sanitized, pendingEntity.timestamp, mapOf())
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to insert pending message", e)
            }
        }
    }
    
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        chatDao.markMessagesAsRead(chatId, currentUserId = currentUserId)
        val conversation = chatDao.getConversation(chatId)
        conversation?.let {
            val newMap = it.unreadCounts.toMutableMap()
            newMap[currentUserId] = 0
            chatDao.updateUnreadCounts(chatId, newMap)
        }
    }
    
    suspend fun acknowledgeMessages(chatId: String, messages: List<MessageEntity>) {
        if (messages.isEmpty()) return
        
        messages.forEach { msg ->
            WebSocketClient.sendReadReceipt(msg.messageId, msg.senderId)
        }
        
        markMessagesAsRead(chatId, currentUserId ?: return)
    }
    
    suspend fun uploadMedia(data: String, filename: String): String? {
        try {
             val body = mapOf("data" to data, "filename" to filename)
             val response = chatService.uploadMedia(body)
             if (response.isSuccessful) {
                 return response.body()?.get("url")
             }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Upload failed", e)
        }
        return null
    }


    suspend fun fetchConversations(userId: String) {
        try {
            Log.d("ChatRepository", "Fetching conversations for user: $userId")
            val response = chatService.getConversations(userId)
            if (response.isSuccessful) {
                val convs = response.body() ?: emptyList()
                Log.d("ChatRepository", "Received ${convs.size} conversations from API")

                convs.forEach { dto ->
                    val participantIds = dto.participants.map { it.userId }

                    val entity = ConversationEntity(
                        chatId = dto.chatId,
                        lastMessage = dto.lastMessage?.snippet ?: "",
                        lastMessageTimestamp = dto.lastMessage?.timestamp ?: dto.updatedAt,
                        unreadCounts = dto.unreadCounts,
                        participants = participantIds,
                        updatedAt = dto.updatedAt
                    )

                    val participants = dto.participants.map { pDto ->

                        val existing = chatDao.getParticipantSync(pDto.userId)
                        ParticipantEntity(
                            userId = pDto.userId,
                            name = pDto.name.takeIf { it.isNotBlank() } ?: existing?.name ?: "User",
                            profilePictureUrl = pDto.profilePictureUrl ?: existing?.profilePictureUrl,
                            isOnline = existing?.isOnline ?: false,
                            lastSeen = existing?.lastSeen,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }

                    chatDao.insertConversationWithParticipants(entity, participants)

                    participants.forEach { participant ->
                        if (participant.userId != userId &&
                            (participant.name == "User" || participant.profilePictureUrl.isNullOrEmpty())) {
                            repositoryScope.launch {
                                fetchAndCacheParticipant(participant.userId)
                            }
                        }
                    }
                }
            } else {
                Log.e("ChatRepository", "fetchConversations failed with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "fetchConversations failed", e)
        }
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }


    private suspend fun handleWebSocketEvent(event: Any) {
        when (event) {
            is ConversationUpdateDto -> {
                Log.d("ChatRepository", "Received ConversationUpdate: ${event.chatId}")
                val safeParticipants = event.participants.orEmpty()
                
                val entity = ConversationEntity(
                    chatId = event.chatId,
                    lastMessage = event.lastMessage,
                    lastMessageTimestamp = event.lastMessageTimestamp ?: isoNow(),
                    unreadCounts = event.unreadCounts,
                    participants = safeParticipants.map { it.userId },
                    updatedAt = event.updatedAt
                )
                val participants = safeParticipants.map { dto ->
                    // Preserve existing data if available
                    val existing = chatDao.getParticipantSync(dto.userId)
                    ParticipantEntity(
                        userId = dto.userId,
                        name = dto.name.takeIf { it.isNotBlank() } ?: existing?.name ?: "User",
                        profilePictureUrl = dto.profilePictureUrl ?: existing?.profilePictureUrl,
                        isOnline = existing?.isOnline ?: false,
                        lastSeen = existing?.lastSeen,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                chatDao.insertConversationWithParticipants(entity, participants)

                // Fetch missing profiles
                participants.forEach { participant ->
                    val myId = currentUserId ?: WebSocketClient.myUserId
                    if (participant.userId != myId &&
                        (participant.name == "User" || participant.profilePictureUrl.isNullOrEmpty())) {
                        fetchAndCacheParticipant(participant.userId)
                    }
                }
            }
            is MessageDto -> {
                Log.d("ChatRepository", "Received Message: ${event.messageId}")
                chatDao.insertMessage(event.toEntity())
                updateLocalConversation(event)
            }
            is MessageStatusUpdateDto -> {
                Log.d("ChatRepository", "Received StatusUpdate: ${event.messageId} -> ${event.status}")
                chatDao.updateMessageStatus(event.messageId, event.status)
            }
            is ReadReceiptDto -> {
                Log.d("ChatRepository", "Received ReadReceipt: ${event.messageId}")
                chatDao.updateMessageStatus(event.messageId, MessageStatus.READ)
            }
            is PresenceDto -> {
                Log.d("ChatRepository", "Received Presence: ${event.userId} -> ${event.status}")
                val isOnline = event.status.equals("ONLINE", ignoreCase = true)
                chatDao.updateUserPresence(event.userId, isOnline, System.currentTimeMillis())
            }
            else -> {
                Log.d("ChatRepository", "Ignoring unknown event type: ${event.javaClass.simpleName}")
            }
        }
    }
    
    private suspend fun updateLocalConversation(message: MessageDto) {
        val chatId = message.chatId ?: WebSocketClient.generateChatId(message.senderId, message.recipientId ?: "")
        
        val conversation = chatDao.getConversation(chatId)
        
        if (conversation == null) {

            val participants = chatId.split("_").filter { it.isNotEmpty() }
            
            val newConversation = ConversationEntity(
                chatId = chatId,
                lastMessage = message.content,
                lastMessageTimestamp = message.timestamp,
                unreadCounts = emptyMap(),
                participants = participants,
                updatedAt = message.timestamp
            )
            
            val participantEntities = mutableListOf<ParticipantEntity>()
            for (participantUserId in participants) {
                var existing = chatDao.getParticipantSync(participantUserId)

                if (existing == null || existing.name == "User" || existing.profilePictureUrl.isNullOrEmpty()) {
                    existing = fetchAndCacheParticipant(participantUserId)
                }

                participantEntities.add(
                    existing ?: ParticipantEntity(
                        userId = participantUserId,
                        name = "User",
                        profilePictureUrl = null,
                        isOnline = false,
                        lastSeen = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            
            chatDao.insertConversationWithParticipants(newConversation, participantEntities)
        }
        
        val existingOrNew = chatDao.getConversation(chatId) ?: return

        val newCounts = existingOrNew.unreadCounts.toMutableMap()
        val myId = currentUserId ?: WebSocketClient.myUserId
        
        if (message.recipientId == myId) {
            val count = newCounts.getOrPut(myId!!) { 0 }
            newCounts[myId] = count + 1
        }
        
        chatDao.updateConversationSnippet(
            chatId,
            message.content ?: "",
            message.timestamp,
            newCounts
        )
    }
    
    private fun MessageDto.toEntity(): MessageEntity {
        val resolvedRecipientId = recipientId ?: if (senderId == currentUserId) "" else currentUserId ?: ""
        val resolvedChatId = chatId ?: WebSocketClient.generateChatId(senderId, resolvedRecipientId)
        
        return MessageEntity(
            messageId = messageId,
            chatId = resolvedChatId,
            senderId = senderId,
            recipientId = resolvedRecipientId,
            content = content,
            timestamp = timestamp,
            status = status,
            type = type,
            metadata = metadata
        )
    }
}
