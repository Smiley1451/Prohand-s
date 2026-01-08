package com.anand.prohands.network

import com.anand.prohands.data.ReviewRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReviewApi {
    @POST("api/reviews")
    suspend fun submitReview(@Body reviewRequest: ReviewRequest): Response<Unit>
}
