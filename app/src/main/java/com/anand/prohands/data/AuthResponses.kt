package com.anand.prohands.data

data class LoginResponse(
    val token: String?,
    val userId: String?,
    val mfaRequired: Boolean
)

data class MfaResponse(
    val token: String,
    val userId: String
)
