package com.anand.prohands.viewmodel

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.data.chat.*
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(
    private val repository: ChatRepository,
    val currentUserId: String, 
    private val recipientId: String,
    private val chatId: String
) : ViewModel() {

    // Raw messages from DB
    private val _messagesFlow = repository.getMessagesForChat(chatId)

    // UI State: Messages grouped by date with separators
    val uiState: StateFlow<ChatUiState> = _messagesFlow.map { messages ->
        val grouped = mutableListOf<ChatItem>()
        var lastDate = ""
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        messages.forEach { message ->
            val date = try {
                // Parse ISO 8601 timestamp compatible with API 24+
                isoFormat.parse(message.timestamp.substringBefore('.').substringBefore('Z')) ?: Date()
            } catch (_: Exception) {
                Date()
            }
            
            val dateStr = dateFormat.format(date)
            
            if (dateStr != lastDate) {
                val displayDate = when {
                    isToday(date) -> "Today"
                    isYesterday(date) -> "Yesterday"
                    else -> displayFormat.format(date)
                }
                grouped.add(ChatItem.DateSeparator(displayDate))
                lastDate = dateStr
            }
            grouped.add(ChatItem.Message(message))
        }
        ChatUiState(items = grouped, messageCount = messages.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    // Recipient Details (Name, Avatar, Presence)
    val recipientUser: StateFlow<ParticipantEntity?> = repository.getParticipant(recipientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Presence state - updated in real-time from WebSocket
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSeenTimestamp = MutableStateFlow<Long?>(null)
    val lastSeenTimestamp: StateFlow<Long?> = _lastSeenTimestamp.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Loading states
    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    // Track if we should scroll to bottom (new message arrived)
    private val _shouldScrollToBottom = MutableStateFlow(false)
    val shouldScrollToBottom: StateFlow<Boolean> = _shouldScrollToBottom.asStateFlow()

    private var recipientTypingJob: Job? = null
    private var myTypingJob: Job? = null
    private var amITyping = false
    private var currentPage = 0
    private val pageSize = 50

    init {
        repository.currentUserId = currentUserId
        
        // Connect WebSocket and ensure connection
        val token = ProHandsApplication.instance.sessionManager.getAuthToken() ?: ""
        WebSocketClient.connect(currentUserId, token)

        // Initial load of history
        loadInitialMessages()

        // Load participant with local-first strategy
        loadParticipantInfo()

        // Observe all WebSocket events
        observeWebSocketEvents()

        // Observe messages for read receipts
        observeMessagesForReadReceipts()

        // Update online status from participant entity
        viewModelScope.launch {
            recipientUser.collect { participant ->
                if (participant != null) {
                    _isOnline.value = participant.isOnline
                    _lastSeenTimestamp.value = participant.lastSeen
                }
            }
        }
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                repository.loadHistory(chatId)
                currentPage = 0
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading initial messages", e)
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    /**
     * Load more messages when user scrolls to top.
     * Uses pagination to load older messages.
     */
    fun loadMoreMessages() {
        if (_isLoadingHistory.value || !_hasMoreMessages.value) return

        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                currentPage++
                val newMessagesCount = repository.loadHistoryPage(chatId, currentPage, pageSize)
                if (newMessagesCount < pageSize) {
                    _hasMoreMessages.value = false
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading more messages", e)
                currentPage-- // Revert on error
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    private fun loadParticipantInfo() {
        viewModelScope.launch {
            // Check if participant exists in DB
            val existingParticipant = repository.getParticipantSync(recipientId)

            if (existingParticipant == null || existingParticipant.name.isBlank() || existingParticipant.name == "User") {
                // No cached data or invalid data - fetch immediately
                repository.fetchAndCacheParticipant(recipientId)
            } else {
                // Have cached data - set initial online status
                _isOnline.value = existingParticipant.isOnline
                _lastSeenTimestamp.value = existingParticipant.lastSeen

                // Refresh in background if stale (older than 1 hour)
                val cacheAgeMs = System.currentTimeMillis() - existingParticipant.lastUpdated
                val oneHourMs = 3600000L
                if (cacheAgeMs > oneHourMs) {
                    repository.refreshParticipantIfStale(recipientId, oneHourMs)
                }
            }
        }
    }

    /**
     * Observe all WebSocket events for this chat.
     * Updates UI in real-time without needing to refresh.
     */
    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            WebSocketClient.events.collect { event ->
                when (event) {
                    // Typing events
                    is TypingEventDto -> {
                        if (event.senderId == recipientId) {
                            _isTyping.value = event.isTyping

                            recipientTypingJob?.cancel()
                            if (event.isTyping) {
                                // Safety timeout to clear typing indicator
                                recipientTypingJob = launch {
                                    delay(5000)
                                    _isTyping.value = false
                                }
                            }
                        }
                    }

                    // Presence events
                    is PresenceDto -> {
                        if (event.userId == recipientId) {
                            val isNowOnline = event.status.equals("ONLINE", ignoreCase = true)
                            _isOnline.value = isNowOnline
                            if (!isNowOnline) {
                                _lastSeenTimestamp.value = System.currentTimeMillis()
                            }
                            Log.d("ChatViewModel", "Presence update: ${event.userId} is ${event.status}")
                        }
                    }

                    // New message received - trigger scroll to bottom
                    is MessageDto -> {
                        if (event.chatId == chatId ||
                            WebSocketClient.generateChatId(event.senderId, event.recipientId ?: "") == chatId) {
                            _shouldScrollToBottom.value = true
                            Log.d("ChatViewModel", "New message received in chat")
                        }
                    }

                    // Connection status could be tracked here
                    else -> {}
                }
            }
        }
    }

    // Monitors messages to automatically send read receipts for incoming unread messages
    private fun observeMessagesForReadReceipts() {
        viewModelScope.launch {
            _messagesFlow.collect { messages ->
                val unreadMessages = messages.filter {
                    it.senderId != currentUserId && it.status != MessageStatus.READ
                }

                if (unreadMessages.isNotEmpty()) {
                    repository.acknowledgeMessages(chatId, unreadMessages)
                }
            }
        }
    }

    /**
     * Called when UI has scrolled to bottom after a new message.
     * Resets the scroll trigger.
     */
    fun onScrolledToBottom() {
        _shouldScrollToBottom.value = false
    }

    fun sendMessage(content: String, type: MessageType = MessageType.TEXT) {
        viewModelScope.launch {
            repository.sendMessage(recipientId, content, type)
            // Trigger scroll to bottom for sent message
            _shouldScrollToBottom.value = true

            // Stop my typing status immediately when sent
            if (amITyping) {
                amITyping = false
                WebSocketClient.sendTypingStatus(recipientId, false)
                myTypingJob?.cancel()
            }
        }
    }
    
    fun sendMediaMessage(uri: Uri, type: MessageType, context: android.content.Context) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Sending media message, URI: $uri, type: $type")

                // 1. Convert URI to Base64
                val base64 = uriToBase64(uri, context)
                if (base64 == null) {
                    Log.e("ChatViewModel", "Failed to convert image to base64")
                    return@launch
                }

                Log.d("ChatViewModel", "Base64 conversion successful, length: ${base64.length}")

                // 2. Upload
                val extension = when (type) {
                    MessageType.IMAGE -> "jpg"
                    MessageType.VIDEO -> "mp4"
                    MessageType.VOICE -> "m4a"
                    else -> "bin"
                }
                val mimeType = when (type) {
                    MessageType.IMAGE -> "image/jpeg"
                    MessageType.VIDEO -> "video/mp4"
                    MessageType.VOICE -> "audio/mp4"
                    else -> "application/octet-stream"
                }
                val filename = "${type.name.lowercase()}_${System.currentTimeMillis()}.$extension"
                val dataUrl = "data:$mimeType;base64,$base64"

                Log.d("ChatViewModel", "Uploading media: $filename")
                val uploadedUrl = repository.uploadMedia(dataUrl, filename)

                // 3. Send Message
                if (uploadedUrl != null) {
                    Log.d("ChatViewModel", "Upload successful, URL: $uploadedUrl")
                    sendMessage(uploadedUrl, type)
                } else {
                    Log.e("ChatViewModel", "Media upload failed - null URL returned")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in sendMediaMessage", e)
            }
        }
    }

    /**
     * Send media from a file path directly (used for camera captures)
     */
    fun sendMediaFromFile(file: java.io.File, type: MessageType) {
        viewModelScope.launch {
            try {
                if (!file.exists() || file.length() == 0L) {
                    Log.e("ChatViewModel", "File doesn't exist or is empty: ${file.absolutePath}")
                    return@launch
                }

                Log.d("ChatViewModel", "Sending media from file: ${file.absolutePath}, size: ${file.length()}")

                // Read file and convert to base64
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                Log.d("ChatViewModel", "File read successful, base64 length: ${base64.length}")

                // Determine mime type
                val extension = when (type) {
                    MessageType.IMAGE -> "jpg"
                    MessageType.VIDEO -> "mp4"
                    MessageType.VOICE -> "m4a"
                    else -> "bin"
                }
                val mimeType = when (type) {
                    MessageType.IMAGE -> "image/jpeg"
                    MessageType.VIDEO -> "video/mp4"
                    MessageType.VOICE -> "audio/mp4"
                    else -> "application/octet-stream"
                }
                val filename = "${type.name.lowercase()}_${System.currentTimeMillis()}.$extension"
                val dataUrl = "data:$mimeType;base64,$base64"

                Log.d("ChatViewModel", "Uploading: $filename")
                val uploadedUrl = repository.uploadMedia(dataUrl, filename)

                if (uploadedUrl != null) {
                    Log.d("ChatViewModel", "Upload successful: $uploadedUrl")
                    sendMessage(uploadedUrl, type)
                } else {
                    Log.e("ChatViewModel", "Upload failed - null URL")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending media from file", e)
            }
        }
    }

    private suspend fun uriToBase64(uri: Uri, context: android.content.Context): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatViewModel", "Converting URI to base64: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("ChatViewModel", "Failed to open input stream for URI: $uri")
                return@withContext null
            }
            val bytes = inputStream.readBytes()
            inputStream.close()

            if (bytes.isEmpty()) {
                Log.e("ChatViewModel", "Read 0 bytes from URI")
                return@withContext null
            }

            Log.d("ChatViewModel", "Read ${bytes.size} bytes from URI")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error converting URI to base64", e)
            null
        }
    }

    fun onUserTyping() {
        // Send typing status if we haven't recently
        if (!amITyping) {
            amITyping = true
            WebSocketClient.sendTypingStatus(recipientId, true)
        }
        
        // Reset the timer that will stop typing
        myTypingJob?.cancel()
        myTypingJob = viewModelScope.launch {
            delay(2000) // Stop typing after 2 seconds of inactivity
            amITyping = false
            WebSocketClient.sendTypingStatus(recipientId, false)
        }
    }
    
    fun setRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    /**
     * Send a voice message from a recorded file.
     */
    fun sendVoiceMessage(audioFile: java.io.File) {
        viewModelScope.launch {
            try {
                // Read file and convert to base64
                val bytes = audioFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUrl = "data:audio/mp4;base64,$base64"
                val filename = "voice_${System.currentTimeMillis()}.m4a"

                Log.d("ChatViewModel", "Uploading voice message: $filename, size: ${bytes.size}")

                // Upload to server
                val uploadedUrl = repository.uploadMedia(dataUrl, filename)

                if (uploadedUrl != null) {
                    Log.d("ChatViewModel", "Voice uploaded successfully: $uploadedUrl")
                    sendMessage(uploadedUrl, MessageType.VOICE)
                } else {
                    Log.e("ChatViewModel", "Voice upload failed")
                }

                // Clean up the temp file
                audioFile.delete()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending voice message", e)
            }
        }
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val d = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val d = Calendar.getInstance().apply { time = date }
        return yesterday.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    }
}

data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val messageCount: Int = 0
)

sealed class ChatItem {
    data class Message(val message: MessageEntity) : ChatItem()
    data class DateSeparator(val date: String) : ChatItem()
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val currentUserId: String,
    private val recipientId: String,
    private val chatId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, currentUserId, recipientId, chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
