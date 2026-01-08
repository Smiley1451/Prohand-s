package com.anand.prohands.data.chat

data class ChatMessage(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val recipientId: String,
    val type: String = "TEXT",
    val content: String,
    val status: String, // Options: SENT, DELIVERED, READ
    val timestamp: String,
    val metadata: Map<String, Any> = emptyMap()
)
