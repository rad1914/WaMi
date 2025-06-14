// @path: app/src/main/java/com/radwrld/wami/network/SocketManager.kt
package com.radwrld.wami.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Message
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.util.NotificationUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class SocketManager(private val context: Context) {

    private val gson = Gson()
    private val serverConfigStorage = ServerConfigStorage(context)
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>()
    val messageStatusUpdates = _messageStatusUpdates.asSharedFlow()

    private val socket = ApiClient.getSocket() ?: ApiClient.initializeSocket(context).let { ApiClient.getSocket() }

    init {
        socket?.apply {
            on("whatsapp-message") { args ->
                Log.d("SocketManager", "Message: ${args[0]}")
                val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
                try {
                    gson.fromJson<List<MessageHistoryItem>>(args[0].toString(), type)
                        .firstOrNull()?.let { msgDto ->
                            // Always emit to the flow for UI updates
                            _incomingMessages.tryEmit(mapToMessage(msgDto))

                            // Don't show notifications for your own outgoing messages
                            if (msgDto.isOutgoing > 0) {
                                return@let
                            }

                            // Show a notification for the incoming message
                            NotificationUtils.showNotification(
                                context = context,
                                jid = msgDto.jid,
                                // Use the new 'name' field, with the JID as a fallback
                                contactName = msgDto.name ?: msgDto.jid,
                                // Provide default text for media messages
                                message = msgDto.text ?: "New media received",
                                messageId = msgDto.id
                            )
                        }
                } catch (e: Exception) {
                    Log.e("SocketManager", "Error processing 'whatsapp-message'", e)
                }
            }

            on("whatsapp-message-status-update") { args ->
                Log.d("SocketManager", "Status update: ${args[0]}")
                (args[0] as? JSONObject)?.let {
                    _messageStatusUpdates.tryEmit(
                        MessageStatusUpdate(it.getString("id"), it.getString("status"))
                    )
                }
            }

            on(io.socket.client.Socket.EVENT_CONNECT) { Log.i("SocketManager", "Connected") }
            on(io.socket.client.Socket.EVENT_DISCONNECT) { Log.w("SocketManager", "Disconnected") }
            on(io.socket.client.Socket.EVENT_CONNECT_ERROR) { Log.e("SocketManager", "Error: ${it.getOrNull(0)}") }
        }
    }

    fun connect() { if (socket?.connected() == false) ApiClient.connectSocket() }
    fun disconnect() { ApiClient.disconnectSocket() }

    private fun mapToMessage(dto: MessageHistoryItem): Message {
        // FIXED: Construct absolute media URL
        val baseUrl = serverConfigStorage.getCurrentServer().removeSuffix("/")
        return Message(
            id = dto.id,
            jid = dto.jid,
            text = dto.text,
            isOutgoing = dto.isOutgoing > 0,
            status = dto.status,
            timestamp = dto.timestamp,
            name = dto.name, // Populate name field
            senderName = dto.name, // Populate senderName field
            mediaUrl = dto.mediaUrl?.let { url -> "$baseUrl$url" },
            mimetype = dto.mimetype,
            quotedMessageId = dto.quotedMessageId,
            quotedMessageText = dto.quotedMessageText
        )
    }

    data class MessageStatusUpdate(val id: String, val status: String)
}
