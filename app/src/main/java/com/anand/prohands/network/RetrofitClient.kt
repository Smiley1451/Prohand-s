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
//    const val BASE_URL = "https://unsufferably-heimish-tashina.ngrok-free.dev/"
const val BASE_URL = "https://unsufferably-heimish-tashina.ngrok-free.dev/"
    private const val CERT_HOST = "unsufferably-heimish-tashina.ngrok-free.dev"
    private const val CERT_PIN_PLACEHOLDER = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    // Removed the manual init requirement. We will pull SessionManager from the Application instance.

    private val okHttpClient: OkHttpClient by lazy {
        // Access SessionManager directly from the Application instance
        val sessionManager = ProHandsApplication.instance.sessionManager
        val authInterceptor = AuthInterceptor(sessionManager)

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS) // Updated to 120s
            .readTimeout(120, TimeUnit.SECONDS)    // Updated to 120s
            .writeTimeout(120, TimeUnit.SECONDS)   // Updated to 120s
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = Level.BODY
            }
            clientBuilder.addInterceptor(logging)
        } else {
            val certificatePinner = CertificatePinner.Builder()
                .add(CERT_HOST, CERT_PIN_PLACEHOLDER)
                .build()
            clientBuilder.certificatePinner(certificatePinner)
        }

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
