package com.anand.prohands.data.chat

data class UserPresence(
    val online: Boolean,
    val lastSeen: String
)

data class MediaSignature(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String,
    val folder: String? = null
)
