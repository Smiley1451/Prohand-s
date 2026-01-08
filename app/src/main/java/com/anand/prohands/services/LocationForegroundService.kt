package com.anand.prohands.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.R
import com.anand.prohands.data.LocationUpdateRequest
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.utils.SessionManager
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager
    private lateinit var profileApi: ProfileApi

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Use the singleton instance from Application
        // Check if application is ProHandsApplication to avoid ClassCastException
        sessionManager = try {
             (application as ProHandsApplication).sessionManager
        } catch (e: Exception) {
             SessionManager(applicationContext)
        }
        
        profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProHands Location Service")
            .setContentText("Tracking your location to find nearby jobs")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority to be less intrusive
            .build()
        
        // Important: startForeground must be called within 5 seconds of the service starting
        // on Android 8+ (Oreo).
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException on Android 12+
        }
        
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        // Location update API is disabled - not registering location updates to save resources
        // To re-enable, uncomment the code below

        /*
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(10)
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userId = sessionManager.getUserId()
                    if (userId != null) {
                        serviceScope.launch {
                            try {
                                profileApi.updateLocation(
                                    userId,
                                    LocationUpdateRequest(location.latitude, location.longitude)
                                )
                            } catch (e: Exception) {
                                // Handle exception
                            }
                        }
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        */
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ProHands Location",
                NotificationManager.IMPORTANCE_LOW // Lower importance for background service
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ProHandsLocationChannel"
        private const val NOTIFICATION_ID = 1
    }
}
