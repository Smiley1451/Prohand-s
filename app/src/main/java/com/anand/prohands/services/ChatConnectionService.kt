package com.anand.prohands.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anand.prohands.MainActivity
import com.anand.prohands.R
import com.anand.prohands.data.chat.MessageDto
import com.anand.prohands.network.WebSocketClient
import com.anand.prohands.utils.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class ChatConnectionService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ChatConnectionServiceChannel"
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, ChatConnectionService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatConnectionService::class.java))
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): ChatConnectionService = this@ChatConnectionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        createNotificationChannel()
        observeMessages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProHands Messenger")
            .setContentText("Connecting for live messages...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .build()
        
        startForeground(NOTIFICATION_ID, notification)

        val userId = intent?.getStringExtra(EXTRA_USER_ID) ?: sessionManager.getUserId()
        val token = sessionManager.getAuthToken()
        
        if (!userId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            WebSocketClient.connect(userId, token)
        } else {
            Log.w("ChatConnectionService", "Stopping service: No user ID or Token provided.")
            stopSelf()
        }

        return START_STICKY
    }

    private fun observeMessages() {
        serviceScope.launch {
            WebSocketClient.events.collectLatest { event ->
                if (event is MessageDto) {
                    // Don't show notification for own messages
                    if (event.senderId != sessionManager.getUserId()) {
                        showNotification(event.senderId, event.content)
                    }
                }
            }
        }
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optionally, you can add extras to navigate to the specific chat
            putExtra("navigateTo", "chat/$title")
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title) // Sender's ID or name
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Service"
            val descriptionText = "Keeps chat connection live for real-time messages"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel all coroutines in this scope
        WebSocketClient.disconnect()
    }
}
