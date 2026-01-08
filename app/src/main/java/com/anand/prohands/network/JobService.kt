package com.anand.prohands.network

import com.anand.prohands.data.JobRequest
import com.anand.prohands.data.JobResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface JobService {

    @POST("jobs")
    suspend fun createJob(@Body jobRequest: JobRequest): Response<JobResponse>

    @GET("jobs")
    suspend fun getAllJobs(): Response<List<JobResponse>>

    @GET("jobs/feed")
    suspend fun getJobFeed(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<List<JobResponse>>

    @GET("jobs/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): Response<JobResponse>

    @PUT("jobs/{jobId}")
    suspend fun updateJob(
        @Path("jobId") jobId: String, 
        @Body jobRequest: JobRequest
    ): Response<JobResponse>

    @DELETE("jobs/{jobId}")
    suspend fun deleteJob(@Path("jobId") jobId: String): Response<Unit>

    @PATCH("jobs/{jobId}/status")
    suspend fun updateStatus(
        @Path("jobId") jobId: String, 
        @Query("status") status: String
    ): Response<Unit>

    @GET("jobs/provider/{providerId}")
    suspend fun getJobsByProvider(@Path("providerId") providerId: String): Response<List<JobResponse>>
}