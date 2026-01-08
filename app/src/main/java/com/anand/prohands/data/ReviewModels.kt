package com.anand.prohands.data

import com.google.gson.annotations.SerializedName

data class ReviewRequest(
    @SerializedName("workerId") val workerId: String,
    @SerializedName("reviewerId") val reviewerId: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("reviewText") val reviewText: String,
    @SerializedName("punctualityScore") val punctualityScore: Int,
    @SerializedName("qualityScore") val qualityScore: Int,
    @SerializedName("behaviourScore") val behaviourScore: Int,
    @SerializedName("jobId") val jobId: String
)
