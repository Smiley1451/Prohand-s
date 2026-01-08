package com.anand.prohands.network

import com.anand.prohands.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // If a token exists, add it to the Authorization header automatically
        sessionManager.getAuthToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // Add the user ID to a custom header for backend services that need it
        sessionManager.getUserId()?.let { userId ->
            requestBuilder.addHeader("X-User-Id", userId)
        }

        return chain.proceed(requestBuilder.build())
    }
}
