// @path: app/src/main/java/com/radwrld/wami/repository/WhatsAppRepository.kt
package com.radwrld.wami.repository

import android.content.Context
// ++ FIX: Added the missing import for the .toUri() extension function.
import androidx.core.net.toUri
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.network.SendMessageRequest
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

    fun getBaseUrl(): String = serverUrl

    suspend fun refreshAndGetConversations(): Result<List<Contact>> {
        return runCatching {
            val conversationsDto = api.getConversations()
            val contacts = conversationsDto.map { conversation ->
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
            conversationStorage.saveConversations(contacts)
            contacts
        }
    }

    fun getCachedConversations(): List<Contact> {
        return conversationStorage.getConversations()
    }

    fun updateAndSaveConversations(updatedList: List<Contact>) {
        conversationStorage.saveConversations(updatedList)
    }
    
    suspend fun getMessageHistory(jid: String): Result<List<Message>> {
        return runCatching {
            val encodedJid = URLEncoder.encode(jid, "UTF-8")
            val msgs = api.getHistory(encodedJid).map {
                Message(
                    id                = it.id,
                    jid               = it.jid,
                    text              = it.text,
                    type              = it.type,
                    isOutgoing        = it.isOutgoing > 0,
                    status            = it.status,
                    timestamp         = it.timestamp,
                    name              = it.name,
                    senderName        = it.name,
                    mediaUrl          = it.mediaUrl?.let { path -> "$serverUrl$path" },
                    mimetype          = it.mimetype,
                    quotedMessageId   = it.quotedMessageId,
                    quotedMessageText = it.quotedMessageText,
                    reactions         = it.reactions ?: emptyMap(),
                    mediaSha256       = it.mediaSha256
                )
            }
            messageStorage.saveMessages(jid, msgs)
            msgs
        }
    }

    suspend fun sendTextMessage(jid: String, text: String, tempId: String) =
        runCatching {
            api.sendMessage(SendMessageRequest(jid, text, tempId)).also {
                if (!it.success) throw Exception(it.error ?: "Unknown error")
            }
        }

    suspend fun sendMediaMessage(jid: String, tempId: String, file: File, caption: String?) =
        runCatching {
            // The toUri() function is now resolved by the added import.
            val mime = context.contentResolver.getType(file.toUri())
            val body = file.asRequestBody(mime?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, body)

            api.sendMedia(
                jid = jid.toRequestBody("text/plain".toMediaTypeOrNull()),
                caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
                tempId = tempId.toRequestBody("text/plain".toMediaTypeOrNull()),
                file = part
            ).also {
                if (!it.success) throw Exception(it.error ?: "Unknown error")
            }
        }

    suspend fun sendReaction(jid: String, messageId: String, fromMe: Boolean, emoji: String) =
        runCatching {
            api.sendReaction(SendReactionRequest(jid, messageId, fromMe, emoji))
        }
}
