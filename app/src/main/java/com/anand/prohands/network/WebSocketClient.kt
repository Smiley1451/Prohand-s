package com.anand.prohands.network

import android.annotation.SuppressLint
import android.util.Log
import com.anand.prohands.data.chat.*
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.net.URLEncoder
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow

@SuppressLint("CheckResult")
object WebSocketClient {

    private var stompClient: StompClient? = null
    private val gson = Gson()
    private val compositeDisposable = CompositeDisposable()

    // Using SharedFlow to emit events to Repository/ViewModel
    private val _events = MutableSharedFlow<Any>(replay = 1)
    val events = _events

    private val scope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null

    var myUserId: String? = null
        private set

    private var myToken: String? = null

    private var reconnectAttempts = 0
    private var isConnecting = false

    // Outgoing in-memory queue for messages when socket is disconnected
    private val outgoingQueue: ArrayDeque<MessageDto> = ArrayDeque()
    private val MAX_QUEUE_SIZE = 100
    private var sslFailureCount = 0
    private val MAX_SSL_RETRIES = 3

    // Logic: (a < b) ? (a + '_' + b) : (b + '_' + a)
    fun generateChatId(a: String, b: String): String {
        return if (a < b) "${a}_$b" else "${b}_$a"
    }

    fun connect(userId: String, token: String) {
        // If already connected with same user, do nothing
        if (stompClient?.isConnected == true && myUserId == userId) return
        if (isConnecting && myUserId == userId) return

        myUserId = userId
        myToken = token
        // Reset reconnect attempts on fresh connect request
        reconnectAttempts = 0
        sslFailureCount = 0

        establishConnection(userId, token)
    }

    private fun establishConnection(userId: String, token: String) {
        // Stop previous client but preserve myUserId/myToken so prepareMessage can still use them
        closeExistingClientPreserveUser()
        isConnecting = true

        compositeDisposable.clear()

        val httpUrl = RetrofitClient.BASE_URL
        // Convert https/http to wss/ws and append path + query param
        // Handle trailing slash to avoid double slash
        val cleanBaseUrl = if (httpUrl.endsWith("/")) httpUrl.dropLast(1) else httpUrl
        val wsBaseUrl = cleanBaseUrl.replace("https://", "wss://").replace("http://", "ws://")

        val encodedUser = try { URLEncoder.encode(userId, "UTF-8") } catch (_: Exception) { userId }

        val wsUrl = "$wsBaseUrl/ws/websocket?userId=$encodedUser"

        Log.d("WebSocketClient", "Connecting to $wsUrl with user $userId")

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

        // Lifecycle events
        val lifecycleDisposable = stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .subscribe({ lifecycleEvent ->
                try {
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.OPENED -> {
                            Log.d("WebSocketClient", "Stomp connection opened")
                            isConnecting = false
                            reconnectAttempts = 0
                            sslFailureCount = 0
                            subscribeToTopics()
                            startAppHeartbeat()
                            // flush queued messages
                            flushOutgoingQueue()
                        }
                        LifecycleEvent.Type.ERROR -> {
                            Log.e("WebSocketClient", "Connection error", lifecycleEvent.exception)
                            isConnecting = false
                            handleLifecycleException(lifecycleEvent.exception)
                            scheduleReconnect()
                        }
                        LifecycleEvent.Type.CLOSED -> {
                            Log.d("WebSocketClient", "Stomp connection closed")
                            isConnecting = false
                            stopAppHeartbeat()
                            scheduleReconnect()
                        }
                        else -> {}
                    }
                } catch (t: Throwable) {
                    Log.e("WebSocketClient", "Error handling lifecycle event", t)
                }
             }, { t ->
                 // Ensure we don't crash on unhandled errors from lifecycle stream
                 Log.e("WebSocketClient", "Lifecycle stream error", t)
                 try {
                     isConnecting = false
                     stopAppHeartbeat()
                     handleLifecycleException(t)
                     scheduleReconnect()
                } catch (_: Throwable) {}
             })
         compositeDisposable.add(lifecycleDisposable)

        // Connect without headers
        try {
            stompClient?.connect()
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Failed to initiate connect", e)
            handleLifecycleException(e)
            scheduleReconnect()
        }
    }

    private fun handleLifecycleException(t: Throwable?) {
        // Detect SSL handshake problems and back off more aggressively
        var cause: Throwable? = t
        while (cause != null) {
            if (cause is javax.net.ssl.SSLHandshakeException) {
                sslFailureCount++
                Log.e("WebSocketClient", "SSL handshake exception detected (count=$sslFailureCount)", cause)
                if (sslFailureCount > MAX_SSL_RETRIES) {
                    // Emit fatal event and stop reconnect attempts by clearing myUserId
                    val msg = cause.message ?: "SSL error"
                    scope.launch { _events.emit(com.anand.prohands.network.SocketErrorDto("SSL_HANDSHAKE_FAILED", msg)) }
                    Log.e("WebSocketClient", "Max SSL retries exceeded. Stopping further reconnect attempts.")
                    // Do not clear stompClient here; just prevent further reconnects
                    myUserId = null
                }
                return
            }
            cause = cause.cause
        }
    }

    private fun scheduleReconnect() {
        val userId = myUserId ?: return
        val token = myToken ?: return

        // If SSL failures exceeded, do not reconnect
        if (sslFailureCount > MAX_SSL_RETRIES) {
            Log.e("WebSocketClient", "Not scheduling reconnect because SSL failures exceeded max retries")
            return
        }

        reconnectAttempts++
        // Exponential backoff: min(30000, 1000 * 2^attempts)
        val delayMs = min(30000.0, 1000.0 * 2.0.pow(min(6, reconnectAttempts).toDouble())).toLong()

        Log.d("WebSocketClient", "Scheduling reconnect attempt $reconnectAttempts in ${delayMs}ms")

        scope.launch {
            delay(delayMs)
            // Only reconnect if we are still supposed to be connected (myUserId is set)
            if (myUserId != null && stompClient?.isConnected != true) {
                establishConnection(userId, token)
            }
        }
    }

    private fun startAppHeartbeat() {
        stopAppHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                try {
                    if (stompClient?.isConnected == true) {
                        stompClient?.send("/app/chat.heartbeat", "")
                            ?.subscribeOn(Schedulers.io())
                            ?.subscribe({ /* no-op */ }, { Log.e("WebSocketClient", "Heartbeat failed", it) })
                            ?.let { compositeDisposable.add(it) }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Heartbeat error", e)
                }
                delay(30000) // 30 seconds
            }
        }
    }

    private fun stopAppHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun subscribeToTopics() {
        stompClient?.let { client ->
            // 1. Subscribe to Live Messages
            val msgDisp = client.topic("/user/queue/messages")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val msg = gson.fromJson(topicMessage.payload, MessageDto::class.java)
                        scope.launch { _events.emit(msg) }
                        // Automatic ACK: Delivered
                        sendAck("/app/chat.delivered", msg.messageId, msg.senderId)
                    } catch (e: Exception) { Log.e("WebSocketClient", "Msg error", e) }
                }, { Log.e("WebSocketClient", "Msg sub error", it) })

            // 2. Subscribe to Read Receipts
            val readDisp = client.topic("/user/queue/read-receipt")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val receipt = gson.fromJson(topicMessage.payload, ReadReceiptDto::class.java)
                        scope.launch { _events.emit(receipt) }
                    } catch (e: Exception) { Log.e("WebSocketClient", "Read error", e) }
                }, { Log.e("WebSocketClient", "Read sub error", it) })

            // 3. Subscribe to Presence
            val presDisp = client.topic("/user/queue/presence")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val p = gson.fromJson(topicMessage.payload, PresenceDto::class.java)
                        scope.launch { _events.emit(p) }
                    } catch (e: Exception) { Log.e("WebSocketClient", "Presence error", e) }
                }, { Log.e("WebSocketClient", "Presence sub error", it) })

            // 4. Subscribe to Typing
            val typingDisp = client.topic("/user/queue/typing")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val t = gson.fromJson(topicMessage.payload, TypingEventDto::class.java)
                        scope.launch { _events.emit(t) }
                    } catch (e: Exception) { Log.e("WebSocketClient", "Typing error", e) }
                }, { Log.e("WebSocketClient", "Typing sub error", it) })

            // 5. Subscribe to Conversations (Snippets)
            val convDisp = client.topic("/user/queue/conversations")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val c = gson.fromJson(topicMessage.payload, ConversationUpdateDto::class.java)
                        scope.launch { _events.emit(c) }
                    } catch (e: Exception) { Log.e("WebSocketClient", "Conv error", e) }
                }, { Log.e("WebSocketClient", "Conv sub error", it) })

            // 6. Subscribe to Status Updates
            val statusDisp = client.topic("/user/queue/status-updates")
                .subscribeOn(Schedulers.io())
                .subscribe({ topicMessage ->
                    try {
                        val s = gson.fromJson(topicMessage.payload, MessageStatusUpdateDto::class.java)
                        scope.launch { _events.emit(s) }
                    } catch (e: Exception) { Log.e("WebSocketClient", "Status error", e) }
                }, { Log.e("WebSocketClient", "Status sub error", it) })

            compositeDisposable.addAll(msgDisp, readDisp, presDisp, typingDisp, convDisp, statusDisp)
        }
    }

    // Creates the DTO but does not send it. Returns null if user not connected/set.
    fun prepareMessage(recipientId: String, text: String, type: MessageType = MessageType.TEXT): MessageDto? {
        val currentUserId = myUserId
        if (currentUserId == null) {
            Log.w("WebSocketClient", "prepareMessage called but myUserId is null; call connect() first")
            return null
        }
         // Keep sanitization minimal: trim and collapse many newlines
         val sanitized = text.trim().replace(Regex("\n{3,}"), "\n\n")

         if (sanitized.isEmpty()) {
             Log.d("WebSocketClient", "Prepared message empty after sanitization; not creating DTO")
             return null
         }

         Log.d("WebSocketClient", "Preparing message to $recipientId: '[...snip]' len=${sanitized.length}")
         val chatId = generateChatId(currentUserId, recipientId)
         return MessageDto(
             messageId = "id-${UUID.randomUUID().toString().take(7)}",
             chatId = chatId,
             senderId = currentUserId,
             recipientId = recipientId,
             content = sanitized,
             type = type,
             timestamp = java.time.Instant.now().toString(),
             status = MessageStatus.SENT
         )
     }

    // enqueue when cannot send
    private fun enqueueOutgoing(msg: MessageDto) {
        synchronized(outgoingQueue) {
            if (outgoingQueue.size >= MAX_QUEUE_SIZE) {
                // drop oldest
                val dropped = outgoingQueue.removeFirstOrNull()
                Log.w("WebSocketClient", "Outgoing queue full; dropping oldest message ${dropped?.messageId}")
            }
            outgoingQueue.addLast(msg)
            Log.d("WebSocketClient", "Enqueued outgoing message ${msg.messageId}; queueSize=${outgoingQueue.size}")
        }
    }

    private fun flushOutgoingQueue() {
        synchronized(outgoingQueue) {
            if (outgoingQueue.isEmpty()) return
            Log.d("WebSocketClient", "Flushing outgoing queue of size ${outgoingQueue.size}")
            val toSend = ArrayList<MessageDto>(outgoingQueue)
            outgoingQueue.clear()
            toSend.forEach { msg ->
                try {
                    // send directly without re-enqueue on failure to avoid infinite loops; failures are logged
                    sendStompMessageRaw(msg)
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Failed to flush message ${msg.messageId}", e)
                }
            }
        }
    }

    private fun sendStompMessageRaw(msg: MessageDto) {
        try {
            stompClient?.send("/app/chat.send", gson.toJson(msg))
                ?.subscribeOn(Schedulers.io())
                ?.subscribe({ Log.d("WebSocketClient", "Flushed message sent: ${msg.messageId}") }, { Log.e("WebSocketClient", "Flush send error", it) })
                ?.let { compositeDisposable.add(it) }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Raw send failed", e)
        }
    }

    // Sends a prepared message DTO
    fun sendStompMessage(msg: MessageDto) {
        if (stompClient?.isConnected == true) {
            try {
                stompClient?.send("/app/chat.send", gson.toJson(msg))
                    ?.subscribeOn(Schedulers.io())
                    ?.subscribe({
                         // Successfully sent to socket
                         Log.d("WebSocketClient", "Message sent: ${msg.messageId}")
                    }, {
                        Log.e("WebSocketClient", "Send error", it)
                        // On send error, enqueue as fallback
                        enqueueOutgoing(msg)
                    })?.let { compositeDisposable.add(it) }
            } catch (e: Exception) {
                Log.e("WebSocketClient", "Failed to send message", e)
                enqueueOutgoing(msg)
            }
        } else {
             Log.w("WebSocketClient", "Not connected, cannot send message; enqueuing")
             enqueueOutgoing(msg)
        }
    }

    // For compatibility if needed, or helper
    fun sendMessage(recipientId: String, text: String, type: MessageType = MessageType.TEXT): MessageDto? {
        val msg = prepareMessage(recipientId, text, type)
        if (msg == null) {
            Log.w("WebSocketClient", "sendMessage aborted: prepareMessage returned null (user not connected or content empty)")
            return null
        }
        sendStompMessage(msg)
        return msg
    }

    fun sendTypingStatus(recipientId: String, isTyping: Boolean) {
        val currentUserId = myUserId ?: return

        val event = mapOf(
            "recipientId" to recipientId,
            "isTyping" to isTyping
        )

        try {
            if (stompClient?.isConnected == true) {
                stompClient?.send("/app/chat.typing", gson.toJson(event))
                    ?.subscribeOn(Schedulers.io())
                    ?.subscribe({ /* no-op */ }, { Log.e("WebSocketClient", "Typing send failed", it) })
                    ?.let { compositeDisposable.add(it) }
            } else {
                Log.w("WebSocketClient", "Typing send skipped: not connected")
            }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Typing send exception", e)
        }
    }

    fun sendReadReceipt(messageId: String, senderId: String) {
        sendAck("/app/chat.read", messageId, senderId)
    }

    private fun sendAck(destination: String, messageId: String, senderId: String) {
        val ack = mapOf("messageId" to messageId, "senderId" to senderId)
        try {
            if (stompClient?.isConnected == true) {
                stompClient?.send(destination, gson.toJson(ack))
                    ?.subscribeOn(Schedulers.io())
                    ?.subscribe({ /* no-op */ }, { Log.e("WebSocketClient", "Ack send failed", it) })
                    ?.let { compositeDisposable.add(it) }
            } else {
                Log.w("WebSocketClient", "Ack skipped: not connected")
            }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Ack exception", e)
        }
    }

    fun disconnect() {
        stopAppHeartbeat()
        try {
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.w("WebSocketClient", "Exception while disconnecting", e)
        }
        stompClient = null
        compositeDisposable.clear()
        isConnecting = false
        myUserId = null
        myToken = null
        synchronized(outgoingQueue) { outgoingQueue.clear() }
    }

    private fun closeExistingClientPreserveUser() {
        stopAppHeartbeat()
        try {
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.w("WebSocketClient", "Error disconnecting previous client", e)
        }
        stompClient = null
        compositeDisposable.clear()
        isConnecting = false
    }

    // Small helper so callers can check connection state
    fun isConnected(): Boolean {
        return stompClient?.isConnected == true
    }
}
