package com.anand.prohands.data.chat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type


data class ConversationUpdateDto(
    val chatId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: String?,
    val unreadCounts: Map<String, Int>,
    val participants: List<ParticipantDto>?,
    val updatedAt: String
)


data class LastMessageDto(
    val messageId: String?,
    val senderId: String?,
    val snippet: String?,
    val timestamp: String?
)


data class ConversationDto(
    val chatId: String,
    @JsonAdapter(ParticipantsDeserializer::class)
    val participants: List<ParticipantDto>,
    val lastMessage: LastMessageDto?, 
    val unreadCounts: Map<String, Int>,
    val updatedAt: String
)

data class ParticipantDto(
    val userId: String,
    val name: String = "User",
    val profilePictureUrl: String? = null
)


class ParticipantsDeserializer : JsonDeserializer<List<ParticipantDto>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<ParticipantDto> {
        val list = mutableListOf<ParticipantDto>()
        if (json.isJsonArray) {
            json.asJsonArray.forEach { element ->
                if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                    list.add(ParticipantDto(userId = element.asString))
                } else if (element.isJsonObject) {
                    try {
                        val dto = context.deserialize<ParticipantDto>(element, ParticipantDto::class.java)
                        list.add(dto)
                    } catch (e: Exception) { }
                }
            }
        }
        return list
    }
}


data class MessageDto(
    val messageId: String,
    val chatId: String? = null,
    val senderId: String,
    val recipientId: String? = null,
    val content: String,
    val timestamp: String,
    val status: MessageStatus,
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String>? = null
)

data class TypingEventDto(
    val senderId: String,
    val recipientId: String,
    val isTyping: Boolean
)

data class MessageStatusUpdateDto(
    val messageId: String,
    val chatId: String,
    val status: MessageStatus
)

data class ReadReceiptDto(
    val messageId: String,
    val senderId: String
)

data class DeliveryConfirmationDto(
    val messageId: String,
    val senderId: String
)

data class PresenceDto(
    val userId: String,
    val status: String 
)

data class MediaSignRequest(
    val filename: String,
    val contentType: String
)

data class MediaSignResponse(
    val uploadUrl: String,
    val publicId: String,
    val apiKey: String,
    val signature: String,
    val timestamp: Long
)

data class SyncResponse(
    val messages: List<MessageDto>,
    val statusUpdates: List<MessageStatusUpdateDto>
)
