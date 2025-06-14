// SocketManager.kt

package com.radwrld.wami.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/**
 * Manages Socket.IO connection and events for real-time communication.
 * This class centralizes all socket event handling.
 */
class SocketManager(private val context: Context) {

    // --- Public Flows for Observers (ViewModels) ---
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>()
    val messageStatusUpdates = _messageStatusUpdates.asSharedFlow()

    private val socket = ApiClient.getSocket() ?: run {
        Log.e("SocketManager", "Socket not initialized, trying to initialize.")
        ApiClient.initializeSocket(context)
        ApiClient.getSocket()
    }
    private val gson = Gson()

    init {
        registerListeners()
    }

    private fun registerListeners() {
        socket?.on("whatsapp-message") { args ->
            Log.d("SocketManager", "Received 'whatsapp-message': ${args[0]}")
            // Backend sends an array of messages
            val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
            val messages: List<MessageHistoryItem> = gson.fromJson(args[0].toString(), type)
            
            messages.firstOrNull()?.let { msgDto ->
                // Backend sends `fromMe` but we use `isOutgoing` in the model
                val message = mapDtoToDomain(msgDto.copy(isOutgoing = if (msgDto.isOutgoing > 0) 1 else 0))
                _incomingMessages.tryEmit(message)
            }
        }

        socket?.on("whatsapp-message-status-update") { args ->
            Log.d("SocketManager", "Received 'whatsapp-message-status-update': ${args[0]}")
            val data = args[0] as JSONObject
            val update = MessageStatusUpdate(
                id = data.getString("id"),
                status = data.getString("status")
            )
            _messageStatusUpdates.tryEmit(update)
        }

        socket?.on(io.socket.client.Socket.EVENT_CONNECT) {
            Log.i("SocketManager", "Socket Connected!")
        }
        socket?.on(io.socket.client.Socket.EVENT_DISCONNECT) {
            Log.w("SocketManager", "Socket Disconnected!")
        }
        socket?.on(io.socket.client.Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketManager", "Socket Connection Error: ${args.getOrNull(0)}")
        }
    }

    fun connect() {
        if (socket?.connected() == false) {
            ApiClient.connectSocket()
        }
    }

    fun disconnect() {
        ApiClient.disconnectSocket()
    }

    /** Maps the network DTO to the app's domain model. */
    private fun mapDtoToDomain(dto: MessageHistoryItem): Message {
        return Message(
            id = dto.id,
            jid = dto.jid,
            text = dto.text,
            isOutgoing = dto.isOutgoing > 0, // Convert Int to Boolean
            status = dto.status,
            timestamp = dto.timestamp,
            mediaUrl = dto.mediaUrl,
            mimetype = dto.mimetype,
            quotedMessageId = dto.quotedMessageId,
            quotedMessageText = dto.quotedMessageText
        )
    }

    data class MessageStatusUpdate(val id: String, val status: String)
}
