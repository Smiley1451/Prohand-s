package com.anand.prohands.data.chat

data class ChatMessageDto(
    val recipientId: String,
    val type: String = "TEXT", // Options: TEXT, IMAGE, VIDEO, VOICE
    val content: String,
    val metadata: Map<String, Any> = emptyMap(),
    val status: String = "SENT"
)
