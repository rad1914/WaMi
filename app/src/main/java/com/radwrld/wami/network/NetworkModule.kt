package com.radwrld.wami.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.MainActivity
import com.radwrld.wami.R
import com.radwrld.wami.data.ContactRepository
import com.radwrld.wami.data.MessageRepository
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.util.NotificationUtils
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// =================================================================================================
// SECTION: Domain Models
// =================================================================================================

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val jid: String,
    val text: String?,
    val isOutgoing: Boolean,
    val type: String? = "conversation",
    val status: String = "sending",
    val timestamp: Long = System.currentTimeMillis(),
    val name: String? = null,
    val senderName: String? = null,
    val mediaUrl: String? = null,
    val localMediaPath: String? = null,
    val mimetype: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val mediaSha256: String? = null
) {
    fun hasMedia(): Boolean = !mediaUrl.isNullOrBlank()
    fun isVideo(): Boolean = mimetype?.startsWith("video/") == true
    fun hasText(): Boolean = !text.isNullOrBlank()
    fun isSticker(): Boolean = type == "sticker"
}


// =================================================================================================
// SECTION: API Data Models & Mapper
// =================================================================================================

data class SendResponse(
    val success: Boolean,
    val messageId: String?,
    val tempId: String?,
    val timestamp: Long?,
    val error: String?
)

data class MessageHistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("jid") val jid: String,
    @SerializedName("text") val text: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("isOutgoing") val isOutgoing: Int,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("mimetype") val mimetype: String?,
    @SerializedName("quoted_message_id") val quotedMessageId: String?,
    @SerializedName("quoted_message_text") val quotedMessageText: String?,
    @SerializedName("reactions") val reactions: Map<String, Int>?,
    @SerializedName("media_sha256") val mediaSha256: String?
)

fun MessageHistoryItem.toDomain(): Message = Message(
    id = this.id,
    jid = this.jid,
    text = this.text,
    isOutgoing = this.isOutgoing > 0,
    type = this.type,
    status = this.status,
    timestamp = this.timestamp,
    name = this.name,
    senderName = this.name,
    mediaUrl = this.mediaUrl,
    localMediaPath = null,
    mimetype = this.mimetype,
    quotedMessageId = this.quotedMessageId,
    quotedMessageText = this.quotedMessageText,
    reactions = this.reactions ?: emptyMap(),
    mediaSha256 = this.mediaSha256
)

data class Conversation(
    @SerializedName("jid") val jid: String,
    @SerializedName("name") val name: String?,
    @SerializedName("is_group") val isGroupInt: Int = 0,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Long?,
    @SerializedName("unreadCount") val unreadCount: Int?
) {
    val isGroup: Boolean get() = isGroupInt == 1
}

data class SessionResponse(val sessionId: String)

data class StatusResponse(val connected: Boolean, val qr: String?)

data class SendMessageRequest(
    val jid: String,
    val text: String,
    val tempId: String
)

data class SendReactionRequest(
    val jid: String,
    val messageId: String,
    val emoji: String
)

data class SyncResponse(
    val success: Boolean,
    val message: String
)

// =================================================================================================
// SECTION: REST API (Retrofit)
// =================================================================================================

interface WhatsAppApi {
    @GET("history/{jid}")
    suspend fun getHistory(
        @Path("jid", encoded = true) jid: String,
        @Query("limit") limit: Int = 100
    ): List<MessageHistoryItem>

    @POST("history/sync/{jid}")
    suspend fun syncHistory(@Path("jid", encoded = true) jid: String): SyncResponse

    @Streaming
    @GET("media/{messageId}")
    suspend fun downloadFile(@Path("messageId") messageId: String): Response<ResponseBody>

    @POST("send")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendResponse

    @Multipart
    @POST("send/media")
    suspend fun sendMedia(
        @Part("jid") jid: RequestBody,
        @Part("caption") caption: RequestBody?,
        @Part("tempId") tempId: RequestBody,
        @Part file: MultipartBody.Part
    ): SendResponse

    @POST("send/reaction")
    suspend fun sendReaction(@Body request: SendReactionRequest): Response<Void>

    @GET("session/status")
    suspend fun getStatus(): StatusResponse

    @POST("session/create")
    suspend fun createSession(): SessionResponse

    @POST("session/logout")
    suspend fun logout(): Response<Void>

    @GET("chats")
    suspend fun getConversations(): List<Conversation>
}

object ApiClient {
    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var downloadRetrofit: Retrofit? = null

    @Volatile var httpClient: OkHttpClient? = null
        private set

    @Volatile var downloadHttpClient: OkHttpClient? = null
        private set

    private fun authInterceptor(context: Context) = Interceptor { chain ->
        val token = ServerConfigStorage(context.applicationContext).getSessionId()
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        chain.proceed(request)
    }

    private fun buildClient(context: Context, logLevel: HttpLoggingInterceptor.Level, timeouts: Boolean = false): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = logLevel })
            .addInterceptor(authInterceptor(context.applicationContext))
            .apply {
                if (timeouts) {
                    readTimeout(5, TimeUnit.MINUTES)
                    writeTimeout(5, TimeUnit.MINUTES)
                    connectTimeout(2, TimeUnit.MINUTES)
                }
            }
            .build()
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun getBaseUrl(context: Context): String = ServerConfigStorage(context.applicationContext).getCurrentServer()

    fun getInstance(context: Context): WhatsAppApi {
        return retrofit?.create(WhatsAppApi::class.java) ?: synchronized(this) {
            retrofit?.create(WhatsAppApi::class.java) ?: run {
                val safeContext = context.applicationContext
                val client = httpClient ?: buildClient(safeContext, HttpLoggingInterceptor.Level.BODY).also { httpClient = it }
                buildRetrofit(getBaseUrl(safeContext), client).also { retrofit = it }.create(WhatsAppApi::class.java)
            }
        }
    }

    fun getDownloadingInstance(context: Context): WhatsAppApi {
        return downloadRetrofit?.create(WhatsAppApi::class.java) ?: synchronized(this) {
            downloadRetrofit?.create(WhatsAppApi::class.java) ?: run {
                val safeContext = context.applicationContext
                val client = downloadHttpClient ?: buildClient(safeContext, HttpLoggingInterceptor.Level.NONE, timeouts = true).also { downloadHttpClient = it }
                buildRetrofit(getBaseUrl(safeContext), client).also { downloadRetrofit = it }.create(WhatsAppApi::class.java)
            }
        }
    }

    fun close() {
        retrofit = null
        downloadRetrofit = null
        httpClient = null
        downloadHttpClient = null
    }
}


// =================================================================================================
// SECTION: Real-time Sync (Socket.IO)
// =================================================================================================

object Logger {
    fun d(tag: String, msg: String) = Log.d(tag, msg)
    fun i(tag: String, msg: String) = Log.i(tag, msg)
    fun w(tag: String, msg: String) = Log.w(tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Log.e(tag, msg, t)
}

private data class ReactionUpdateDto(val id: String, val jid: String, val reactions: Map<String, Int>)
private data class FullMessageStatusUpdateDto(val jid: String, val id: String, val status: String)

private interface SocketEventHandler {
    suspend fun handleIncomingMessages(json: String)
    suspend fun handleStatusUpdate(json: String)
    suspend fun handleReactionUpdate(json: String)
}

private class DefaultSocketEventHandler(
    private val context: Context,
    private val messageRepo: MessageRepository,
    private val contactRepo: ContactRepository
) : SocketEventHandler {
    private val gson = Gson()

    override suspend fun handleIncomingMessages(json: String) {
        runCatching {
            val listType = object : TypeToken<List<MessageHistoryItem>>() {}.type
            val items: List<MessageHistoryItem> = gson.fromJson(json, listType)
            items.forEach { dto ->
                val msg = dto.toDomain()
                messageRepo.addMessage(msg.jid, msg)
                if (!msg.isOutgoing) {
                    NotificationUtils.showNotification(
                        context = context,
                        jid = msg.jid,
                        contactName = msg.senderName ?: msg.jid,
                        message = msg.text ?: "Media",
                        messageId = msg.id
                    )
                }
            }
        }.onFailure { t ->
            Logger.e(TAG, "Failed processing incoming messages: $json", t)
        }
    }

    override suspend fun handleStatusUpdate(json: String) {
        runCatching {
            val update = gson.fromJson(json, FullMessageStatusUpdateDto::class.java)
            messageRepo.updateMessageStatus(update.jid, update.id, update.status)
        }.onFailure { t ->
            Logger.e(TAG, "Failed processing status update: $json", t)
        }
    }

    override suspend fun handleReactionUpdate(json: String) {
        runCatching {
            val update = gson.fromJson(json, ReactionUpdateDto::class.java)
            messageRepo.updateMessageReactions(update.jid, update.id, update.reactions)
        }.onFailure { t ->
            Logger.e(TAG, "Failed processing reaction update: $json", t)
        }
    }

    companion object { private const val TAG = "DefaultSocketEventHandler" }
}

object SyncManager {
    private const val TAG = "SyncManager"

    @Volatile private var socket: Socket? = null
    private val isInitialized = AtomicBoolean(false)

    private lateinit var appContext: Context
    private lateinit var msgRepo: MessageRepository
    private lateinit var contactRepo: ContactRepository
    private lateinit var handler: SocketEventHandler

    private var retryCount = 0
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisor)

    private val _socketState = MutableStateFlow(false)
    val socketState: StateFlow<Boolean> = _socketState.asStateFlow()

    private val _qrCodeUrl = MutableStateFlow<String?>(null)
    val qrCodeUrl: StateFlow<String?> = _qrCodeUrl.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow(false)
    val authError: StateFlow<Boolean> = _authError.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return

        appContext  = context.applicationContext
        msgRepo     = MessageRepository(appContext)
        contactRepo = ContactRepository(appContext)
        handler     = DefaultSocketEventHandler(appContext, msgRepo, contactRepo)

        val config = ServerConfigStorage(appContext)
        val token  = config.getSessionId().orEmpty()
        if (token.isBlank()) {
            Logger.e(TAG, "Cannot initialize: missing session token")
            isInitialized.set(false)
            return
        }

        try {
            resetLoginState()
            
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                reconnection = false
            }
            socket = IO.socket(config.getCurrentServer(), opts)
            registerListeners()
            Logger.i(TAG, "Initialized with server=${config.getCurrentServer()}")
        } catch (e: URISyntaxException) {
            Logger.e(TAG, "Invalid socket URI", e)
        }
    }
    
    fun resetLoginState() {
        _qrCodeUrl.value = null
        _isAuthenticated.value = false
        _authError.value = false
    }

    fun connect() {
        if (!isInitialized.get()) {
            Logger.w(TAG, "Connect called before initialize()")
            return
        }
        socket?.takeIf { !it.connected() }?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun shutdown() {
        disconnect()
        socket?.off()
        socket = null
        isInitialized.set(false)
        supervisor.cancel()
        Logger.i(TAG, "SyncManager shutdown complete")
    }

    private fun registerListeners() = socket?.apply {
        on(Socket.EVENT_CONNECT) {
            Logger.i(TAG, "Socket connected")
            retryCount = 0
            _socketState.value = true
            _qrCodeUrl.value = null
            _authError.value = false
        }
        on(Socket.EVENT_DISCONNECT) { args ->
            Logger.w(TAG, "Socket disconnected: ${args.getOrNull(0)?.toString().orEmpty()}")
            _socketState.value = false
            _isAuthenticated.value = false

            if (!_authError.value) {
               scheduleReconnect()
            }
        }
        on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.getOrNull(0)?.toString().orEmpty()
            Logger.e(TAG, "Socket error: $error")
            
            if (error.contains("Invalid session ID")) {
                _authError.value = true
                disconnect()
            } else {
                scheduleReconnect()
            }
        }
        on("qr") { args ->
            Logger.i(TAG, "QR event received")
            val qr = args.getOrNull(0) as? String
            _qrCodeUrl.value = qr
        }
        on("authenticated") {
            Logger.i(TAG, "Authenticated event received")
            _qrCodeUrl.value = null
            _isAuthenticated.value = true
        }
        on("disconnected") {
             Logger.w(TAG, "Received server-side disconnect event")
            _isAuthenticated.value = false
        }
        on("whatsapp-message") { args ->
            Logger.d(TAG, "Received whatsapp-message: ${args.getOrNull(0)}")
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handler.handleIncomingMessages(it) }
            }
        }
        on("whatsapp-message-status-update") { args ->
            Logger.d(TAG, "Received status-update: ${args.getOrNull(0)}")
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handler.handleStatusUpdate(it) }
            }
        }
        on("whatsapp-reaction-update") { args ->
            Logger.d(TAG, "Received reaction-update: ${args.getOrNull(0)}")
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handler.handleReactionUpdate(it) }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!isInitialized.get() || _authError.value) return
        retryCount++
        val delayMs = (2.0.pow(retryCount.toDouble()) * 1000L)
            .toLong()
            .coerceAtMost(60_000L)

        scope.launch {
            delay(delayMs)
            Logger.d(TAG, "Reconnecting attempt #$retryCount")
            connect()
        }
    }
}


// =================================================================================================
// SECTION: Android Service
// =================================================================================================

class SyncService : Service() {

    companion object {
        const val ACTION_START = "com.radwrld.wami.sync.ACTION_START"
        const val ACTION_STOP  = "com.radwrld.wami.sync.ACTION_STOP"
        private const val NOTIF_ID    = 1
        private const val CHANNEL_ID  = "SyncServiceChannel"
        private const val CHANNEL_NAME = "Wami Sync Service"
    }

    override fun onCreate() {
        super.onCreate()
        SyncManager.initialize(this)
        Logger.i("SyncService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                buildForegroundNotification()
                Logger.i("SyncService", "Starting—connecting socket")
                SyncManager.connect()
            }
            ACTION_STOP  -> {
                Logger.i("SyncService", "Stopping service")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildForegroundNotification() {
        createChannelIfNeeded()
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wami Conectado")
            .setContentText("Escuchando mensajes en tiempo real")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notif)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("SyncService", "Destroyed—shutting down SyncManager")
        SyncManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
