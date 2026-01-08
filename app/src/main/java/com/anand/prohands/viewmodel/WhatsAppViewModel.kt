package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.data.chat.ChatRepository
import com.anand.prohands.data.chat.MessageDto
import com.anand.prohands.data.chat.MessageStatus
import com.anand.prohands.data.chat.MessageType
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State Management (The "WhatsApp Engine")
// This ViewModel centralizes the logic for managing the conversation list and message history, ensuring it reacts to live updates.
class WhatsAppViewModel(
    private val repository: ChatRepository,
    val currentUserId: String
) : ViewModel() {

    // Current Chat State
    // In a real app with navigation, this might be handled by ChatViewModel, but for global state we track active chat here or just rely on repository
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    // Since we are using Room and Flows in Repository, we don't need to manually manage a list of messages or conversations in memory here.
    // The UI should observe repository.getConversations() and repository.getMessagesForChat(chatId)

    init {
        // Connect to WebSocket on init
        val token = ProHandsApplication.instance.sessionManager.getAuthToken() ?: ""
        WebSocketClient.connect(currentUserId, token)
        
        viewModelScope.launch {
            // Observe incoming messages globally to handle background logic if needed
            WebSocketClient.events.collect { event ->
                if (event is MessageDto) {
                    handleIncomingMessage(event)
                }
            }
        }
    }

    private fun handleIncomingMessage(msg: MessageDto) {
        // logic: If chat is open, append to screen and mark as READ
        // This logic is partially handled by the UI observing the DB.
        // But we need to send the READ receipt if the chat is currently active.
        
        if (_activeChatId.value == msg.chatId) {
            // Mark as read immediately
            WebSocketClient.sendReadReceipt(msg.messageId, msg.senderId)
            
            // Also update local DB to show it as read
            viewModelScope.launch {
                // Safely handle nullable chatId in msg by fallback if needed, though activeChatId check implies it matches
                val targetChatId = msg.chatId ?: _activeChatId.value ?: return@launch
                repository.markMessagesAsRead(targetChatId, currentUserId)
            }
        } else {
             // It's a background message, show notification (not implemented here)
        }
    }

    fun onUserTyping(recipientId: String, text: String) {
        // JS logic: Clear timeout and send typing = true
        // This is handled in the specific ChatViewModel typically, but can be here too.
        if (text.isNotEmpty()) {
            WebSocketClient.sendTypingStatus(recipientId, true)
        } else {
            WebSocketClient.sendTypingStatus(recipientId, false)
        }
    }
}

class WhatsAppViewModelFactory(
    private val repository: ChatRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhatsAppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WhatsAppViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
