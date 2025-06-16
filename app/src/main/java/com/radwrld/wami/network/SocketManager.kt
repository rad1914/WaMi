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

class SocketManager(private val context: Context) {

    private val gson = Gson()
    private val serverConfigStorage = ServerConfigStorage(context)
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>()
    val messageStatusUpdates = _messageStatusUpdates.asSharedFlow()
    
    // ++ Applied suggestion: Added a flow for broadcasting reaction updates to the UI.
    private val _reactionUpdates = MutableSharedFlow<ReactionUpdate>()
    val reactionUpdates = _reactionUpdates.asSharedFlow()

    private val socket = ApiClient.getSocket() ?: ApiClient.initializeSocket(context).let { ApiClient.getSocket() }

    init {
        socket?.apply {
            on("whatsapp-message") { args ->
                Log.d("SocketManager", "Message: ${args[0]}")
                val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
                try {
                    gson.fromJson<List<MessageHistoryItem>>(args[0].toString(), type)
                        .firstOrNull()?.let { msgDto ->
                            _incomingMessages.tryEmit(mapToMessage(msgDto))

                            if (msgDto.isOutgoing > 0) {
                                return@let
                            }

                            NotificationUtils.showNotification(
                                context = context,
                                jid = msgDto.jid,
                                contactName = msgDto.name ?: msgDto.jid,
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
                try {
                    val updateDto = gson.fromJson(args[0].toString(), MessageStatusUpdateDto::class.java)
                    _messageStatusUpdates.tryEmit(
                        MessageStatusUpdate(updateDto.id, updateDto.status)
                    )
                } catch (e: Exception) {
                    Log.e("SocketManager", "Error processing 'whatsapp-message-status-update'", e)
                }
            }
            
            // ++ Applied suggestion: Added a listener for real-time reaction events.
            on("whatsapp-reaction-update") { args ->
                Log.d("SocketManager", "Reaction update: ${args[0]}")
                try {
                    val update = gson.fromJson(args[0].toString(), ReactionUpdate::class.java)
                    _reactionUpdates.tryEmit(update)
                } catch (e: Exception) {
                    Log.e("SocketManager", "Error processing 'whatsapp-reaction-update'", e)
                }
            }

            on(io.socket.client.Socket.EVENT_CONNECT) { Log.i("SocketManager", "Connected") }
            on(io.socket.client.Socket.EVENT_DISCONNECT) { Log.w("SocketManager", "Disconnected") }
            on(io.socket.client.Socket.EVENT_CONNECT_ERROR) { Log.e("SocketManager", "Error: ${it.getOrNull(0)}") }
        }
    }

    fun connect() { if (socket?.connected() == false) ApiClient.connectSocket() }
    fun disconnect() { ApiClient.disconnectSocket() }

    // ++ Applied suggestion: The mapper now correctly handles the new reactions field.
    private fun mapToMessage(dto: MessageHistoryItem): Message {
        val baseUrl = serverConfigStorage.getCurrentServer().removeSuffix("/")
        return Message(
            id = dto.id,
            jid = dto.jid,
            text = dto.text,
            isOutgoing = dto.isOutgoing > 0,
            type = dto.type,
            status = dto.status,
            timestamp = dto.timestamp,
            name = dto.name,
            senderName = dto.name,
            mediaUrl = dto.mediaUrl?.let { url -> "$baseUrl$url" },
            mimetype = dto.mimetype,
            quotedMessageId = dto.quotedMessageId,
            quotedMessageText = dto.quotedMessageText,
            reactions = dto.reactions ?: emptyMap()
        )
    }

    data class MessageStatusUpdate(val id: String, val status: String)
    
    // ++ Applied suggestion: Data class for deserializing reaction update events.
    data class ReactionUpdate(val id: String, val jid: String, val reactions: Map<String, Int>)
}
