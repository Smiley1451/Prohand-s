package com.anand.prohands.data

import com.google.gson.annotations.SerializedName


data class SignupRequest(
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String, // Must be min 8 chars, 1 upper, 1 lower, 1 digit, 1 special char
    @SerializedName("role") val role: String = "STUDENT", // Sending as String to match Enum: STUDENT, TEACHER, ADMIN
    @SerializedName("mfaEnabled") val mfaEnabled: Boolean = false
)

// Matches backend 'LoginRequest.java'
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// Matches backend 'VerifyAccountRequest.java'
data class VerifyAccountRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

// Matches backend 'MfaRequest.java'
data class MfaRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

// Matches backend 'ResetPasswordRequest.java'
data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String // Note: Backend calls this 'newPassword', not 'password'
)

// Matches backend 'RefreshTokenRequest.java'
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)
