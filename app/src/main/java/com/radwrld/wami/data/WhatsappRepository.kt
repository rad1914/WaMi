// @path: app/src/main/java/com/radwrld/wami/data/WhatsappRepository.kt
// @path: app/src/main/java/com/radwrld/wami/repository/WhatsappRepository.kt
package com.radwrld.wami.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.BlockRequest
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.GroupInfo
import com.radwrld.wami.network.Message
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.network.toDomain
import com.radwrld.wami.storage.ConversationStorage
import com.radwrld.wami.storage.MessageStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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
                avatarUrl = "$serverUrl/avatar/${it.jid}",
                isGroup = isGroup
            )
        }.also {
            conversationStorage.mergeConversations(it)
        }
    }

    fun getCachedConversations() = conversationStorage.getConversations()

    suspend fun getMessageHistory(jid: String, before: Long? = null) = runCatching {
        api.getHistory(jid).map { historyItem ->
            historyItem.toDomain().run {
                copy(mediaUrl = mediaUrl?.let { path ->
                    if (path.startsWith("http")) path else "$serverUrl$path"
                })
            }
        }.also { messages ->
            messageStorage.appendMessages(jid, messages)
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

    suspend fun sendReaction(jid: String, messageId: String, emoji: String) =
        runCatching {
            api.sendReaction(SendReactionRequest(jid, messageId, emoji))
        }

    suspend fun blockContact(jid: String) = runCatching {
        api.blockContact(BlockRequest(jid))
    }

    suspend fun unblockContact(jid: String) = runCatching {
        api.unblockContact(BlockRequest(jid))
    }

    suspend fun reportContact(jid: String) = runCatching {
        api.reportContact(BlockRequest(jid))
    }

    suspend fun getGroupInfo(jid: String): Result<GroupInfo> = runCatching {
        api.getGroupInfo(jid)
    }

    suspend fun getCommonGroups(contactJid: String): Result<List<Contact>> = runCatching {
        val allConversations = getCachedConversations()
        val userGroups = allConversations.filter { it.isGroup }
        val commonGroups = mutableListOf<Contact>()

        for (groupContact in userGroups) {
            try {
                val groupInfo = getGroupInfo(groupContact.id).getOrThrow()
                if (groupInfo.participants.any { it.id == contactJid }) {
                    commonGroups.add(groupContact)
                }
            } catch (e: Exception) {
                Log.w("WhatsAppRepository", "Falló al obtener información para el grupo ${groupContact.id}", e)
            }
            delay(300L)
        }
        commonGroups
    }

    suspend fun getStatuses(): Result<List<StatusItem>> = runCatching {
        val contacts = conversationStorage.getConversations().associateBy { it.id }
        api.getStatuses().map { status ->
            status.copy(
                avatarUrl = "$serverUrl/avatar/${status.jid}",
                senderName = contacts[status.jid]?.name ?: status.jid.substringBefore('@'),
                mediaUrl = status.mediaUrl?.let { if (it.startsWith("http")) it else "$serverUrl$it" }
            )
        }
    }

    suspend fun sendStatus(file: File, caption: String?): Result<Boolean> = runCatching {
        val mimeType = context.contentResolver.getType(file.toUri()) ?: "application/octet-stream"
        val filePart = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull())
        )
        val tempId = UUID.randomUUID().toString()
        val response = api.sendStatus(
            caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
            tempId = tempId.toRequestBody("text/plain".toMediaTypeOrNull()),
            file = filePart
        )
        response.success
    }
}
