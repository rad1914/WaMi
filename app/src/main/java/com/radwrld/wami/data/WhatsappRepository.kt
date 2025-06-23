package com.radwrld.wami.repository

import android.content.Context
import androidx.core.net.toUri
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.storage.ConversationStorage
import com.radwrld.wami.storage.MessageStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder

class WhatsAppRepository(private val context: Context) {
    private val api = ApiClient.getInstance(context)
    private val conversationStorage = ConversationStorage(context)
    private val messageStorage = MessageStorage(context)
    private val serverUrl = ApiClient.getBaseUrl(context).removeSuffix("/")

    fun getBaseUrl() = serverUrl

    suspend fun refreshAndGetConversations() = runCatching {
        api.getConversations().map {
            val isGroup = it.jid.endsWith("@g.us")
            Contact(
                id = it.jid,
                name = it.name ?: "Unknown",
                phoneNumber = if (isGroup) null else it.jid.substringBefore('@'),
                lastMessage = it.lastMessage,
                lastMessageTimestamp = it.lastMessageTimestamp,
                unreadCount = it.unreadCount ?: 0,
                avatarUrl = "$serverUrl/avatar/${URLEncoder.encode(it.jid, "UTF-8")}",
                isGroup = isGroup
            )
        }.also { conversationStorage.saveConversations(it) }
    }

    fun getCachedConversations() = conversationStorage.getConversations()

    fun updateAndSaveConversations(updated: List<Contact>) =
        conversationStorage.saveConversations(updated)

    suspend fun getMessageHistory(jid: String, before: Long? = null) = runCatching {
        val timestampCursor = before ?: System.currentTimeMillis() + 1000
        // ++ CORRECCIÓN: Se eliminó el parámetro `before` de la llamada a la API
        api.getHistory(URLEncoder.encode(jid, "UTF-8")).map {
            Message(
                id = it.id,
                jid = it.jid,
                text = it.text,
                type = it.type,
                isOutgoing = it.isOutgoing > 0,
                status = it.status,
                timestamp = it.timestamp,
                name = it.name,
                senderName = it.name,
                mediaUrl = it.mediaUrl?.let { path -> if (path.startsWith("http")) path else "$serverUrl$path" },
                mimetype = it.mimetype,
                quotedMessageId = it.quotedMessageId,
                quotedMessageText = it.quotedMessageText,
                reactions = it.reactions.orEmpty(),
                mediaSha256 = it.mediaSha256
            )
        }.also { messages ->
            if (before != null) {
                 messageStorage.appendMessages(jid, messages)
            } else {
                messageStorage.saveMessages(jid, messages)
            }
        }
    }

    suspend fun sendTextMessage(jid: String, text: String, tempId: String) = runCatching {
        api.sendMessage(SendMessageRequest(jid, text, tempId))
            .takeIf { it.success } ?: error("Send failed")
    }

    suspend fun sendMediaMessage(jid: String, tempId: String, file: File, caption: String?) = runCatching {
        val mimeType = context.contentResolver.getType(file.toUri()) ?: "application/octet-stream"
        val part = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull())
        )
        api.sendMedia(
            jid.toRequestBody("text/plain".toMediaTypeOrNull()),
            caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
            tempId.toRequestBody("text/plain".toMediaTypeOrNull()),
            part
        ).takeIf { it.success } ?: error("Send failed")
    }

    // ++ FUNCIÓN ACTUALIZADA: Se eliminó el parámetro `fromMe`
    suspend fun sendReaction(jid: String, messageId: String, emoji: String) =
        runCatching {
            api.sendReaction(SendReactionRequest(jid, messageId, emoji))
        }
}
