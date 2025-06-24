// @path: app/src/main/java/com/radwrld/wami/network/ApiClient.kt
package com.radwrld.wami.network

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// region Data Classes & Mappers
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
    val fileName: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val mediaSha256: String? = null
) {
    fun hasMedia(): Boolean = !mediaUrl.isNullOrBlank()
    fun isVideo(): Boolean = mimetype?.startsWith("video/") == true
    fun hasText(): Boolean = !text.isNullOrBlank()
    fun isSticker(): Boolean = type == "sticker"
    fun isDocument(): Boolean = type == "document" || type == "audio"
}

data class SendResponse(val success: Boolean, val messageId: String?, val tempId: String?, val timestamp: Long?, val error: String?)
data class SessionResponse(val sessionId: String)
data class StatusResponse(val connected: Boolean, val qr: String?)
data class SendMessageRequest(val jid: String, val text: String, val tempId: String)
data class SendReactionRequest(val jid: String, val messageId: String, val emoji: String)
data class SyncResponse(val success: Boolean, val message: String)

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
    @SerializedName("fileName") val fileName: String?,
    @SerializedName("quoted_message_id") val quotedMessageId: String?,
    @SerializedName("quoted_message_text") val quotedMessageText: String?,
    @SerializedName("reactions") val reactions: Map<String, Int>?,
    @SerializedName("media_sha256") val mediaSha256: String?
)

fun MessageHistoryItem.toDomain(): Message = Message(
    id = this.id, jid = this.jid, text = this.text, isOutgoing = this.isOutgoing > 0,
    type = this.type, status = this.status, timestamp = this.timestamp, name = this.name,
    senderName = this.name, mediaUrl = this.mediaUrl, mimetype = this.mimetype, fileName = this.fileName,
    quotedMessageId = this.quotedMessageId, quotedMessageText = this.quotedMessageText,
    reactions = this.reactions ?: emptyMap(), mediaSha256 = this.mediaSha256
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
// endregion

// region API Definition
interface WhatsAppApi {
    @GET("history/{jid}")
    suspend fun getHistory(@Path("jid", encoded = true) jid: String, @Query("limit") limit: Int = 100): List<MessageHistoryItem>

    @POST("history/sync/{jid}")
    suspend fun syncHistory(@Path("jid", encoded = true) jid: String): SyncResponse

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    @POST("send")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendResponse

    @Multipart
    @POST("send/media")
    suspend fun sendMedia(@Part("jid") jid: RequestBody, @Part("caption") caption: RequestBody?, @Part("tempId") tempId: RequestBody, @Part file: MultipartBody.Part): SendResponse

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
// endregion

// region Networking
object ApiClient {
    @Volatile private var api: WhatsAppApi? = null
    @Volatile private var downloadApi: WhatsAppApi? = null

    @Volatile var downloadHttpClient: OkHttpClient? = null
        private set

    fun getBaseUrl(context: Context): String = ServerConfigStorage(context.applicationContext).getCurrentServer()

    private fun createOkHttpClient(context: Context, useLongTimeouts: Boolean): OkHttpClient {
        val serverConfig = ServerConfigStorage(context.applicationContext)
        val backendHost = try { Uri.parse(serverConfig.getCurrentServer()).host } catch (_: Exception) { null }

        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (useLongTimeouts) HttpLoggingInterceptor.Level.NONE else HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val token = serverConfig.getSessionId()
                val originalRequest = chain.request()

                val requestBuilder = if (!token.isNullOrEmpty() && originalRequest.url.host == backendHost) {
                    originalRequest.newBuilder().header("Authorization", "Bearer $token")
                } else {
                    originalRequest.newBuilder()
                }
                chain.proceed(requestBuilder.build())
            }
            .apply {
                if (useLongTimeouts) {
                    readTimeout(5, TimeUnit.MINUTES)
                    writeTimeout(5, TimeUnit.MINUTES)
                }
            }
            .build()
    }

    private fun createRetrofit(context: Context, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl(context))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getInstance(context: Context): WhatsAppApi =
        api ?: synchronized(this) {
            api ?: createRetrofit(context, createOkHttpClient(context.applicationContext, false))
                .create(WhatsAppApi::class.java).also { api = it }
        }

    fun getDownloadingInstance(context: Context): WhatsAppApi =
        downloadApi ?: synchronized(this) {
            downloadApi ?: run {
                val client = downloadHttpClient ?: createOkHttpClient(context.applicationContext, true).also {
                    downloadHttpClient = it
                }
                createRetrofit(context, client).create(WhatsAppApi::class.java).also { downloadApi = it }
            }
        }

    fun close() {
        api = null
        downloadApi = null
        downloadHttpClient = null
    }
}

object SyncManager {
    private const val TAG = "SyncManager"
    @Volatile private var socket: Socket? = null
    private val isInitialized = AtomicBoolean(false)

    private lateinit var appContext: Context
    private lateinit var msgRepo: MessageRepository
    private lateinit var contactRepo: ContactRepository

    private var retryCount = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

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

        appContext = context.applicationContext
        msgRepo = MessageRepository(appContext)
        contactRepo = ContactRepository(appContext)

        val config = ServerConfigStorage(appContext)
        val token = config.getSessionId()
        if (token.isNullOrBlank()) {
            Log.e(TAG, "Cannot initialize: missing session token")
            isInitialized.set(false)
            return
        }

        try {
            resetLoginState()
            val opts = IO.Options().apply { auth = mapOf("token" to token); reconnection = false }
            socket = IO.socket(config.getCurrentServer(), opts)
            registerListeners()
            Log.i(TAG, "Initialized with server=${config.getCurrentServer()}")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid socket URI", e)
        }
    }

    fun connect() {
        if (!isInitialized.get()) Log.w(TAG, "Connect called before initialize()")
        else socket?.takeIf { !it.connected() }?.connect()
    }

    fun disconnect() = socket?.disconnect()
    fun isConnected(): Boolean = socket?.connected() == true
    fun resetLoginState() {
        _qrCodeUrl.value = null
        _isAuthenticated.value = false
        _authError.value = false
    }

    fun shutdown() {
        disconnect()
        socket?.off()
        socket = null
        isInitialized.set(false)
        scope.coroutineContext.cancelChildren()
        Log.i(TAG, "SyncManager shutdown complete")
    }

    private fun registerListeners() = socket?.apply {
        on(Socket.EVENT_CONNECT) {
            Log.i(TAG, "Socket connected")
            retryCount = 0
            _socketState.value = true
            _qrCodeUrl.value = null
            _authError.value = false
        }
        on(Socket.EVENT_DISCONNECT) {
            Log.w(TAG, "Socket disconnected: ${it.getOrNull(0)}")
            _socketState.value = false
            _isAuthenticated.value = false
            if (!_authError.value) scheduleReconnect()
        }
        on(Socket.EVENT_CONNECT_ERROR) {
            val error = it.getOrNull(0)?.toString() ?: "Unknown error"
            Log.e(TAG, "Socket error: $error")
            if (error.contains("Invalid session ID")) {
                _authError.value = true
                disconnect()
            } else {
                scheduleReconnect()
            }
        }
        on("qr") { _qrCodeUrl.value = it.getOrNull(0) as? String }
        on("authenticated") {
            Log.i(TAG, "Authenticated event received")
            _qrCodeUrl.value = null
            _isAuthenticated.value = true
        }
        on("disconnected") {
            Log.w(TAG, "Received server-side disconnect event")
            _isAuthenticated.value = false
        }
        on("whatsapp-message") { args ->
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handleIncomingMessages(it) }
            }
        }
        on("whatsapp-message-status-update") { args ->
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handleStatusUpdate(it) }
            }
        }
        on("whatsapp-reaction-update") { args ->
            scope.launch {
                args.getOrNull(0)?.toString()?.let { handleReactionUpdate(it) }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!isInitialized.get() || _authError.value) return
        retryCount++
        val delayMs = (2.0.pow(retryCount) * 1000).toLong().coerceAtMost(60_000L)
        scope.launch {
            delay(delayMs)
            Log.d(TAG, "Reconnecting attempt #$retryCount")
            connect()
        }
    }

    // Event Handlers
    private data class ReactionUpdateDto(val id: String, val jid: String, val reactions: Map<String, Int>)
    private data class StatusUpdateDto(val jid: String, val id: String, val status: String)

    private suspend fun handleIncomingMessages(json: String) {
        runCatching {
            val serverUrl = ApiClient.getBaseUrl(appContext)
            val items: List<MessageHistoryItem> = gson.fromJson(json, object : TypeToken<List<MessageHistoryItem>>() {}.type)
            items.forEach { dto ->
                val msg = dto.toDomain().run {
                    copy(mediaUrl = mediaUrl?.let { path ->
                        if (path.startsWith("http")) path else "$serverUrl$path"
                    })
                }

                msgRepo.addMessage(msg.jid, msg)
                if (!msg.isOutgoing) {
                    NotificationUtils.showNotification(
                        context = appContext, jid = msg.jid,
                        contactName = msg.senderName ?: msg.jid, message = msg.text ?: "Media", messageId = msg.id
                    )
                }
            }
        }.onFailure { Log.e(TAG, "Failed processing incoming messages: $json", it) }
    }

    private suspend fun handleStatusUpdate(json: String) {
        runCatching {
            val update: StatusUpdateDto = gson.fromJson(json, StatusUpdateDto::class.java)
            msgRepo.updateMessageStatus(update.jid, update.id, update.status)
        }.onFailure { Log.e(TAG, "Failed processing status update: $json", it) }
    }

    private suspend fun handleReactionUpdate(json: String) {
        runCatching {
            val update: ReactionUpdateDto = gson.fromJson(json, ReactionUpdateDto::class.java)
            msgRepo.updateMessageReactions(update.jid, update.id, update.reactions)
        }.onFailure { Log.e(TAG, "Failed processing reaction update: $json", it) }
    }
}
// endregion

// region Android Service
class SyncService : Service() {
    companion object {
        const val ACTION_START = "com.radwrld.wami.sync.ACTION_START"
        const val ACTION_STOP  = "com.radwrld.wami.sync.ACTION_STOP"
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "SyncServiceChannel"
        private const val CHANNEL_NAME = "Wami Sync Service"
    }

    override fun onCreate() {
        super.onCreate()
        SyncManager.initialize(this)
        Log.i("SyncService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, createNotification())
                Log.i("SyncService", "Starting and connecting socket")
                SyncManager.connect()
            }
            ACTION_STOP  -> {
                Log.i("SyncService", "Stopping service")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("SyncService", "Destroyed - shutting down SyncManager")
        SyncManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wami Conectado")
            .setContentText("Escuchando mensajes en tiempo real.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
// endregion
