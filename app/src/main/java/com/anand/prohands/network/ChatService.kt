package com.anand.prohands.network

import com.anand.prohands.data.chat.ConversationDto
import com.anand.prohands.data.chat.MediaSignRequest
import com.anand.prohands.data.chat.MediaSignResponse
import com.anand.prohands.data.chat.MessageDto
import com.anand.prohands.data.chat.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatService {

    @GET("api/chat/sync")
    suspend fun sync(
        @Query("since") since: String
    ): Response<SyncResponse>

    @GET("api/chat/conversations")
    suspend fun getConversations(
        @Query("userId") userId: String
    ): Response<List<ConversationDto>>

    @GET("api/chat/history/{chatId}")
    suspend fun getHistory(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<MessageDto>>

    @POST("api/chat/media/sign")
    suspend fun signMedia(
        @Body request: MediaSignRequest
    ): Response<MediaSignResponse>
    
    // Helper for the new upload logic if needed, mirroring JS
    // body: { data: base64, filename: name }
    @POST("api/chat/media/upload")
    suspend fun uploadMedia(
        @Body body: Map<String, String>
    ): Response<Map<String, String>> // Returns { url: "..." }
}
