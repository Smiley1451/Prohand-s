package com.anand.prohands.data.chat

sealed interface ChatItem {
    data class Message(val message: ChatMessage) : ChatItem
    data class DateSeparator(val date: String) : ChatItem
}
