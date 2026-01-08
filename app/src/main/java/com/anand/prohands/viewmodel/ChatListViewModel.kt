package com.anand.prohands.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.data.chat.ChatRepository
import com.anand.prohands.data.chat.ConversationWithParticipants
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {

    // Use the filtered query that only returns conversations for the current user
    val conversations: StateFlow<List<ConversationWithParticipants>> = repository.getConversationsForUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Log.d("ChatListViewModel", "Initializing for user: $currentUserId")

        // Set current user in repository
        repository.currentUserId = currentUserId

        // Connect WebSocket for real-time updates
        val token = ProHandsApplication.instance.sessionManager.getAuthToken() ?: ""
        WebSocketClient.connect(currentUserId, token)

        // Initialize chat data
        initializeChatData()
    }

    /**
     * Initialize chat data with proper order:
     * 1. Initialize repository for user (clears old user data if needed)
     * 2. Fetch conversations from API
     * 3. Sync any missed messages
     * 4. Ensure all participant profiles are complete
     */
    private fun initializeChatData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Step 1: Initialize repository for this user (handles user switch)
                repository.initializeForUser(currentUserId)
                Log.d("ChatListViewModel", "Repository initialized for user")

                // Step 2: Sync any missed messages
                repository.syncData()
                Log.d("ChatListViewModel", "Sync data completed")

                // Step 3: Ensure all participant profiles have complete data
                repository.ensureParticipantProfiles(currentUserId)
                Log.d("ChatListViewModel", "Participant profiles ensured")

            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Error initializing chat data", e)
                _error.value = "Failed to load chats. Pull to refresh."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh conversations from the API.
     * Call this when user pulls to refresh.
     */
    fun refreshConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Fetch fresh conversations from API
                repository.fetchConversations(currentUserId)

                // Ensure profiles are complete
                repository.ensureParticipantProfiles(currentUserId)

            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Error refreshing conversations", e)
                _error.value = "Failed to refresh. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ChatListViewModelFactory(
    private val repository: ChatRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
