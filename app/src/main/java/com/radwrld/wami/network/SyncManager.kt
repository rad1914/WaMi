package com.radwrld.wami.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.data.ContactRepository
import com.radwrld.wami.data.MessageRepository
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.MessageHistoryItem
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.util.NotificationUtils
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URISyntaxException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

object Logger {
    fun d(tag: String, msg: String) = Log.d(tag, msg)
    fun i(tag: String, msg: String) = Log.i(tag, msg)
    fun w(tag: String, msg: String) = Log.w(tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Log.e(tag, msg, t)
}

private data class ReactionUpdateDto(val id: String, val jid: String, val reactions: Map<String, Int>)
private data class FullMessageStatusUpdateDto(val jid: String, val id: String, val status: String)

object MessageMapper {
    fun fromDto(dto: MessageHistoryItem): Message = Message(
        id = dto.id,
        jid = dto.jid,
        text = dto.text,
        isOutgoing = dto.isOutgoing > 0,
        type = dto.type,
        status = dto.status,
        timestamp = dto.timestamp,
        name = dto.name,
        senderName = dto.name,
        mediaUrl = dto.mediaUrl,
        localMediaPath = null,
        mimetype = dto.mimetype,
        quotedMessageId = dto.quotedMessageId,
        quotedMessageText = dto.quotedMessageText,
        reactions = dto.reactions ?: emptyMap(),
        mediaSha256 = dto.mediaSha256
    )
}

interface SocketEventHandler {
    suspend fun handleIncomingMessages(json: String)
    suspend fun handleStatusUpdate(json: String)
    suspend fun handleReactionUpdate(json: String)
}

class DefaultSocketEventHandler(
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
                val msg = MessageMapper.fromDto(dto)
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
        supervisor.cancel()
        disconnect()
        socket = null
        isInitialized.set(false)
        Logger.i(TAG, "SyncManager shutdown complete")
    }

    private fun registerListeners() = socket?.apply {
        on(Socket.EVENT_CONNECT) {
            Logger.i(TAG, "Socket connected")
            retryCount = 0
            _socketState.value = true
            _qrCodeUrl.value = null
        }
        on(Socket.EVENT_DISCONNECT) { args ->
            Logger.w(TAG, "Socket disconnected: ${args.getOrNull(0)?.toString().orEmpty()}")
            _socketState.value = false
            _isAuthenticated.value = false
            scheduleReconnect()
        }
        on(Socket.EVENT_CONNECT_ERROR) { args ->
            Logger.e(TAG, "Socket error: ${args.getOrNull(0)?.toString().orEmpty()}")
            scheduleReconnect()
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
        if (!isInitialized.get()) return
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
