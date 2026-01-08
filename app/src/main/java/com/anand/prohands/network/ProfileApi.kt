package com.anand.prohands.network

import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.LocationUpdateRequest
import com.anand.prohands.data.PagedSearchResult
import com.anand.prohands.data.WorkerRecommendationDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileApi {
    @GET("api/clients/{userId}")
    suspend fun getProfile(
        @Path("userId") userId: String
    ): Response<ClientProfileDto>

    @PUT("api/clients/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body profile: ClientProfileDto
    ): Response<ClientProfileDto>

    @Multipart
    @POST("api/clients/{userId}/picture")
    suspend fun uploadProfilePicture(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<ClientProfileDto>

    @POST("api/clients/{userId}/refresh-ai")
    suspend fun triggerAiRefresh(@Path("userId") userId: String): Response<ClientProfileDto>
    
    @GET("api/clients")
    suspend fun searchProfiles(
        @Query("skill") skill: String?,
        @Query("minRating") minRating: Double?
    ): Response<List<ClientProfileDto>>

    @GET("api/clients/search")
    suspend fun searchClients(
        @Query("q") q: String? = null,
        @Query("userid") userid: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("minSimilarity") minSimilarity: Double? = null,
        @Query("sort") sort: String? = null
    ): Response<PagedSearchResult>

    @GET("api/clients/recommendations")
    suspend fun getWorkerRecommendations(
        @Query("jobTitle") jobTitle: String,
        @Query("jobDescription") jobDescription: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<List<WorkerRecommendationDto>>

    @POST("api/clients/{userId}/location")
    suspend fun updateLocation(
        @Path("userId") userId: String,
        @Body locationUpdateRequest: LocationUpdateRequest
    ): Response<Unit>
}
