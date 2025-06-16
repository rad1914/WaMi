// @path: app/src/main/java/com/radwrld/wami/repository/WhatsAppRepository.kt
package com.radwrld.wami.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.network.SendMessageRequest
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

    suspend fun getConversations() = runCatching {
        val serverUrl = ApiClient.getBaseUrl(context).removeSuffix("/")
        api.getConversations().map { conversation ->
            val isGroup = conversation.jid.endsWith("@g.us")
            Contact(
                id = conversation.jid,
                name = conversation.name ?: "Unknown",
                phoneNumber = if (!isGroup) conversation.jid.split('@').first() else null,
                lastMessage = conversation.lastMessage,
                lastMessageTimestamp = conversation.lastMessageTimestamp,
                unreadCount = conversation.unreadCount ?: 0,
                avatarUrl = "$serverUrl/avatar/${conversation.jid}",
                isGroup = isGroup
            )
        }
    }.onFailure { Log.e("Repo", "getConversations", it) }

    suspend fun getMessageHistory(jid: String) = runCatching {
        val serverUrl = ApiClient.getBaseUrl(context).removeSuffix("/")
        val encoded = URLEncoder.encode(jid, "UTF-8")
        val msgs = api.getHistory(encoded, limit = 1000).map {
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

    suspend fun sendMediaMessage(jid: String, uri: Uri, caption: String?) = runCatching {
        val file = createTempFileFromUri(uri)
        val mime = context.contentResolver.getType(uri)
        val body = file.asRequestBody(mime?.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        api.sendMedia(
            jid.toRequestBody("text/plain".toMediaTypeOrNull()),
            caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
            part
        ).also {
            file.delete()
            if (!it.success) throw Exception(it.error ?: "Unknown error")
        }
    }.onFailure { Log.e("Repo", "sendMediaMessage", it) }

    // ++ Applied suggestion: sendReaction is now part of the repository for consistency.
    suspend fun sendReaction(jid: String, messageId: String, fromMe: Boolean, emoji: String) = runCatching {
        val request = SendReactionRequest(jid, messageId, fromMe, emoji)
        api.sendReaction(request)
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inStream = context.contentResolver.openInputStream(uri)
            ?: error("Can't open URI: $uri")
        return File.createTempFile("upload_", ".tmp", context.cacheDir).apply {
            outputStream().use { out -> inStream.use { it.copyTo(out) } }
        }
    }
}
