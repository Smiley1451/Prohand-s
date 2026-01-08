package com.anand.prohands.data

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class JobRequest(
    @SerializedName("providerId") val providerId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("wage") val wage: Double,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("requiredSkills") val requiredSkills: List<String>? = null,
    @SerializedName("numberOfEmployees") val numberOfEmployees: Int
)

data class JobResponse(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("wage") val wage: BigDecimal,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("status") val status: String,
    @SerializedName("providerId") val providerId: String,
    @SerializedName("requiredSkills") val requiredSkills: List<String>
)

data class WorkerRecommendationDto(
    @SerializedName("profile") val profile: ClientProfileDto,
    @SerializedName("matchScore") val matchScore: Int,
    @SerializedName("distanceKm") val distanceKm: Double
)

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double
)
