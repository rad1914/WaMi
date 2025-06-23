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
import kotlin.math.pow

// --- Abstracciones sin cambios ---
object Logger {
    fun d(tag: String, msg: String) = Log.d(tag, msg)
    fun i(tag: String, msg: String) = Log.i(tag, msg)
    fun w(tag: String, msg: String) = Log.w(tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Log.e(tag, msg, t)
}

// DTO para las actualizaciones de reacciones del socket
private data class ReactionUpdateDto(val id: String, val jid: String, val reactions: Map<String, Int>)
// DTO para actualizaciones de estado de mensajes del socket
private data class FullMessageStatusUpdateDto(val jid: String, val id: String, val status: String)

object MessageMapper {
    fun fromDto(dto: MessageHistoryItem): Message = Message( id = dto.id, jid = dto.jid, text = dto.text, isOutgoing = dto.isOutgoing > 0, type = dto.type, status = dto.status, timestamp = dto.timestamp, name = dto.name, senderName = dto.name, mediaUrl = dto.mediaUrl, localMediaPath = null, mimetype = dto.mimetype, quotedMessageId = dto.quotedMessageId, quotedMessageText = dto.quotedMessageText, reactions = dto.reactions ?: emptyMap(), mediaSha256 = dto.mediaSha256 )
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
            val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
            val items: List<MessageHistoryItem> = gson.fromJson(json, type)
            items.forEach { dto ->
                val message = MessageMapper.fromDto(dto)
                messageRepo.addMessage(message.jid, message)
                if (!message.isOutgoing) {
                    NotificationUtils.showNotification(context = context, jid = message.jid, contactName = message.senderName ?: message.jid, message = message.text ?: "Media", messageId = message.id)
                }
            }
        }.onFailure { t -> Logger.e(TAG, "Failed processing incoming messages: $json", t) }
    }
    override suspend fun handleStatusUpdate(json: String) {
        runCatching {
            val update = gson.fromJson(json, FullMessageStatusUpdateDto::class.java)
            messageRepo.updateMessageStatus(update.jid, update.id, update.status)
        }.onFailure { t -> Logger.e(TAG, "Failed processing status update: $json", t) }
    }
    override suspend fun handleReactionUpdate(json: String) {
        runCatching {
            val update = gson.fromJson(json, ReactionUpdateDto::class.java)
            messageRepo.updateMessageReactions(update.jid, update.id, update.reactions)
        }.onFailure { t -> Logger.e(TAG, "Failed processing reaction update: $json", t) }
    }
    companion object { private const val TAG = "DefaultSocketEventHandler" }
}

// --- SyncManager ahora es controlado por SyncService ---
object SyncManager {
    private const val TAG = "SyncManager"

    @Volatile private var socket: Socket? = null
    private lateinit var applicationContext: Context
    private lateinit var messageRepository: MessageRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var eventHandler: SocketEventHandler
    private var retryAttempts = 0
    private var job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val _socketState = MutableStateFlow(false)
    val socketState: StateFlow<Boolean> get() = _socketState.asStateFlow()
    @Volatile private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return

            applicationContext = context.applicationContext
            messageRepository = MessageRepository(applicationContext)
            contactRepository = ContactRepository(applicationContext)
            eventHandler = DefaultSocketEventHandler(applicationContext, messageRepository, contactRepository)

            val config = ServerConfigStorage(applicationContext)
            val token = config.getSessionId()
            if (token.isNullOrEmpty()) {
                Logger.e(TAG, "Cannot initialize: sessionId missing.")
                return
            }

            try {
                // El token de sesión ahora se pasa en `auth` para la conexión del socket
                val opts = IO.Options().apply { auth = mapOf("token" to token) }
                socket = IO.socket(config.getCurrentServer(), opts)
                setupSocketListeners()
                isInitialized = true
                Logger.i(TAG, "SyncManager initialized.")
            } catch (e: URISyntaxException) {
                Logger.e(TAG, "Socket URI syntax error", e)
            }
        }
    }

    fun connect() {
        if (!isInitialized) {
            Logger.w(TAG, "Not initialized; cannot connect.")
            return
        }
        socket?.takeIf { !it.connected() }?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun isConnected(): Boolean = socket?.connected() ?: false

    fun shutdown() {
        job.cancel()
        disconnect()
        isInitialized = false
        Logger.i(TAG, "SyncManager shut down.")
    }

    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT,    onConnect)
            on(Socket.EVENT_DISCONNECT, onDisconnect)
            on(Socket.EVENT_CONNECT_ERROR, onError)
            // Los nombres de los eventos se alinean con los emitidos por el servidor
            on("whatsapp-message", onIncoming)
            on("whatsapp-message-status-update", onStatusUpdate)
            on("whatsapp-reaction-update", onReactionUpdate)
        }
    }

    private val onConnect = Emitter.Listener {
        Logger.i(TAG, "Socket connected.")
        retryAttempts = 0
        _socketState.value = true
    }

    private val onDisconnect = Emitter.Listener { args ->
        val reason = args.getOrNull(0)?.toString() ?: "unknown"
        Logger.w(TAG, "Socket disconnected: $reason")
        _socketState.value = false
        if (isInitialized) {
            scheduleReconnect()
        }
    }

    private val onError = Emitter.Listener { args ->
        val error = args.getOrNull(0)?.toString() ?: "unknown"
        Logger.e(TAG, "Socket connection error: $error")
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!isInitialized) return
        retryAttempts++
        val delayMs = (2.0.pow(retryAttempts.toDouble()) * 1000L).toLong().coerceAtMost(60_000L)
        scope.launch {
            delay(delayMs)
            Logger.d(TAG, "Reconnecting (attempt #$retryAttempts)...")
            connect()
        }
    }

    private val onIncoming = Emitter.Listener { args ->
        Logger.d(TAG, "Received whatsapp-message: ${args.getOrNull(0)}")
        scope.launch { args.getOrNull(0)?.toString()?.let { eventHandler.handleIncomingMessages(it) } }
    }

    private val onStatusUpdate = Emitter.Listener { args ->
        Logger.d(TAG, "Received whatsapp-message-status-update: ${args.getOrNull(0)}")
        scope.launch { args.getOrNull(0)?.toString()?.let { eventHandler.handleStatusUpdate(it) } }
    }

    private val onReactionUpdate = Emitter.Listener { args ->
        Logger.d(TAG, "Received whatsapp-reaction-update: ${args.getOrNull(0)}")
        scope.launch { args.getOrNull(0)?.toString()?.let { eventHandler.handleReactionUpdate(it) } }
    }
}
