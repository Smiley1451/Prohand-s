package com.anand.prohands.network

import com.anand.prohands.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    // Signup returns AuthResponse (contains username)
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    // Login returns LoginResponse (contains mfaRequired flag)
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Account verification returns empty body (Void)
    @POST("auth/verify")
    suspend fun verifyAccount(@Body request: VerifyAccountRequest): Response<Unit>

    // MFA Verification typically returns the final LoginResponse with tokens
    @POST("auth/mfa")
    suspend fun verifyMfa(@Body request: MfaRequest): Response<LoginResponse>

    // Token Refresh returns AuthResponse
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    // Request Password Reset (Backend uses RequestParam/Query for email)
    @POST("auth/reset-password-request")
    suspend fun requestPasswordReset(@Query("email") email: String): Response<Unit>

    // Confirm Password Reset (Backend uses JSON body)
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>
    
    // Logout
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
