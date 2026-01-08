package com.anand.prohands

import android.app.Application
import com.anand.prohands.data.local.AppDatabase
import com.anand.prohands.utils.SessionManager
import io.reactivex.plugins.RxJavaPlugins

class ProHandsApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var database: AppDatabase
        private set

    companion object {
        lateinit var instance: ProHandsApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Install RxJava global error handler to prevent uncaught Rx exceptions crashing the app
        RxJavaPlugins.setErrorHandler { throwable ->
            // Log and swallow common non-fatal Rx errors from STOMP lib
            android.util.Log.e("ProHandsApplication", "Unhandled Rx error", throwable)
        }
        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
    }
}
