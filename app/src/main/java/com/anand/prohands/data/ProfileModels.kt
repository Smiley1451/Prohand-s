package com.anand.prohands.data

import com.google.gson.annotations.SerializedName

data class ClientProfileDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("profilePictureUrl") val profilePictureUrl: String?,
    @SerializedName("experienceLevel") val experienceLevel: String?,
    @SerializedName("recommendationFlag") val recommendationFlag: Boolean = false,
    @SerializedName("averageRating") val averageRating: Double = 0.0,
    @SerializedName("jobSuccessRate") val jobSuccessRate: Double = 0.0,
    @SerializedName("recommendedWagePerHour") val recommendedWagePerHour: Double?,
    @SerializedName("aiGeneratedSummary") val aiGeneratedSummary: String?,
    @SerializedName("topReviewKeywords") val topReviewKeywords: List<String>?,
    @SerializedName("skills") val skills: List<String>?,
    @SerializedName("profileStrengthScore") val profileStrengthScore: Int?,
    @SerializedName("totalReviews") val totalReviews: Int = 0,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("lastAiUpdate") val lastAiUpdate: String?, // Added field
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("profileCompletionPercent") val profileCompletionPercent: Int = 0
)
