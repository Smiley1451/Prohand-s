package com.anand.prohands.network

import android.content.Context
import com.anand.prohands.BuildConfig
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.utils.SessionManager
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://admin.prohands.tech/"
    private const val CERT_HOST = "admin.prohands.tech"

    private val okHttpClient: OkHttpClient by lazy {
        // Access SessionManager directly from the Application instance
        val sessionManager = ProHandsApplication.instance.sessionManager
        val authInterceptor = AuthInterceptor(sessionManager)

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = Level.BODY
            }
            clientBuilder.addInterceptor(logging)
        }
        
        // Removed the invalid CertificatePinner that was causing connection failures in signed APK.
        // If certificate pinning is required, add a valid pin for admin.prohands.tech.

        clientBuilder.build()
    }

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
