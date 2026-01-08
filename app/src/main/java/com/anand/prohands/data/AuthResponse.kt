package com.anand.prohands.data

/**
 * General-purpose authentication response, typically used for signup or token refresh.
 */
data class AuthResponse(
    val userId: String,
    val username: String,
    val token: String
)
