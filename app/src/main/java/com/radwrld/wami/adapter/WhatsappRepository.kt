// @path: app/src/main/java/com/radwrld/wami/repository/WhatsAppRepository.kt
package com.radwrld.wami.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.network.MessageHistoryItem
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.SendResponse
import com.radwrld.wami.storage.MessageStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder

class WhatsAppRepository(private val context: Context) {
    private val api = ApiClient.getInstance(context)
    private val storage = MessageStorage(context)
    private val serverUrl = ApiClient.getBaseUrl(context).removeSuffix("/")

    fun getBaseUrl(): String = serverUrl

    suspend fun getConversations() = runCatching {
        api.getConversations().map { conversation ->
            val isGroup = conversation.jid.endsWith("@g.us")
            Contact(
                id = conversation.jid,
                name = conversation.name ?: "Unknown",
                phoneNumber = if (!isGroup) conversation.jid.split('@').first() else null,
                lastMessage = conversation.lastMessage,
                lastMessageTimestamp = conversation.lastMessageTimestamp,
                unreadCount = conversation.unreadCount ?: 0,
                avatarUrl = "$serverUrl/avatar/${URLEncoder.encode(conversation.jid, "UTF-8")}",
                isGroup = isGroup
            )
        }
    }.onFailure { Log.e("Repo", "getConversations", it) }

    suspend fun getMessageHistory(jid: String) = runCatching {
        val encodedJid = URLEncoder.encode(jid, "UTF-8")
        val msgs = api.getHistory(encodedJid, limit = 1000).map {
            Message(
                id                = it.id,
                jid               = it.jid,
                text              = it.text,
                type              = it.type,
                isOutgoing        = it.isOutgoing > 0,
                status            = it.status,
                timestamp         = it.timestamp,
                name              = it.name,
                mediaUrl          = it.mediaUrl?.let { path -> "$serverUrl$path" },
                mimetype          = it.mimetype,
                quotedMessageId   = it.quotedMessageId,
                quotedMessageText = it.quotedMessageText,
                reactions         = it.reactions ?: emptyMap()
            )
        }
        storage.saveMessages(jid, msgs)
        msgs
    }.onFailure { Log.e("Repo", "getMessageHistory: $jid", it) }

    suspend fun sendTextMessage(jid: String, text: String, tempId: String) = runCatching {
        api.sendMessage(SendMessageRequest(jid, text, tempId)).also {
            if (!it.success) throw Exception(it.error ?: "Unknown error")
        }
    }.onFailure { Log.e("Repo", "sendTextMessage", it) }

    // ++ FIX: The function now accepts a File object directly, which is more efficient.
    suspend fun sendMediaMessage(jid: String, tempId: String, file: File, caption: String?) = runCatching {
        // No longer need to create a temporary file, as the ViewModel provides the cached file.
        val mime = context.contentResolver.getType(file.toUri())
        val body = file.asRequestBody(mime?.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        
        api.sendMedia(
            jid = jid.toRequestBody("text/plain".toMediaTypeOrNull()),
            caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
            tempId = tempId.toRequestBody("text/plain".toMediaTypeOrNull()),
            file = part
        ).also {
            // We no longer delete the file, as it's part of the permanent cache.
            if (!it.success) throw Exception(it.error ?: "Unknown error")
        }
    }.onFailure { Log.e("Repo", "sendMediaMessage", it) }

    suspend fun sendReaction(jid: String, messageId: String, fromMe: Boolean, emoji: String) = runCatching {
        val request = SendReactionRequest(jid, messageId, fromMe, emoji)
        api.sendReaction(request)
    }

    // This helper is no longer needed by sendMediaMessage but may be useful elsewhere.
    private fun createTempFileFromUri(uri: Uri): File {
        val inStream = context.contentResolver.openInputStream(uri)
            ?: error("Can't open URI: $uri")
        return File.createTempFile("upload_", ".tmp", context.cacheDir).apply {
            outputStream().use { out -> inStream.use { it.copyTo(out) } }
        }
    }
}
