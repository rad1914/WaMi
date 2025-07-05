// @path: app/src/main/java/com/radwrld/wami/data/WhatsappRepository.kt
// @path: app/src/main/java/com/radwrld/wami/data/WhatsAppRepository.kt
package com.radwrld.wami.data

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.BlockRequest
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.GroupInfo
import com.radwrld.wami.network.Message
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.network.SendResponse
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.network.toDomain
import java.io.File
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class WhatsAppRepository(private val context: Context) {
    private val api = ApiClient.getInstance(context)

    private fun String.toTextRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun File.asFormDataPart(partName: String): MultipartBody.Part {
        val mimeType = context.contentResolver.getType(this.toUri()) ?: "application/octet-stream"
        return MultipartBody.Part.createFormData(partName, this.name, this.asRequestBody(mimeType.toMediaTypeOrNull()))
    }

    fun getBaseUrl() = ApiClient.getBaseUrl(context).removeSuffix("/")

    suspend fun fetchConversations(): Result<List<Contact>> = runCatching {
        api.getConversations().map {
            val isGroup = it.jid.endsWith("@g.us")
            Contact(
                id = it.jid,
                name = it.name ?: "Unknown",
                phoneNumber = if (isGroup) null else it.jid.substringBefore('@'),
                lastMessageTimestamp = it.lastMessageTimestamp,
                unreadCount = it.unreadCount ?: 0,
                avatarUrl = ApiClient.resolveAvatarUrl(context, it.jid),
                isGroup = isGroup
            )
        }
    }

    suspend fun getMessageHistory(jid: String): Result<List<Message>> = runCatching {
        api.getHistory(jid).map { historyItem ->
            historyItem.toDomain().run {
                copy(mediaUrl = ApiClient.resolveUrl(context, mediaUrl))
            }
        }
    }

    suspend fun sendTextMessage(jid: String, text: String, tempId: String): Result<SendResponse> {
        return try {
            val response = api.sendMessage(SendMessageRequest(jid, text, tempId))
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Send failed"))
            }
        } catch (e: Exception) {
            if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    try {

                        val errorResponse = Gson().fromJson(errorBody, SendResponse::class.java)
                        return Result.failure(Exception(errorResponse.error ?: "Send failed. TempID: ${errorResponse.tempId}"))
                    } catch (parseException: Exception) {

                        return Result.failure(e)
                    }
                }
            }
            Result.failure(e)
        }
    }

    suspend fun sendMediaMessage(jid: String, tempId: String, file: File, caption: String?): Result<SendResponse> {
         return try {
            val response = api.sendMedia(
                jid.toTextRequestBody(),
                caption?.toTextRequestBody(),
                tempId.toTextRequestBody(),
                file.asFormDataPart("file")
            )
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Send media failed"))
            }
        } catch (e: Exception) {
            if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        val errorResponse = Gson().fromJson(errorBody, SendResponse::class.java)
                        return Result.failure(Exception(errorResponse.error ?: "Send media failed. TempID: ${errorResponse.tempId}"))
                    } catch (parseException: Exception) {
                        return Result.failure(e)
                    }
                }
            }
            Result.failure(e)
        }
    }

    suspend fun sendReaction(jid: String, messageId: String, emoji: String) =
        runCatching {
            val response = api.sendReaction(SendReactionRequest(jid, messageId, emoji))
            if (!response.success) error(response.error ?: "Failed to send reaction")
        }

    suspend fun blockContact(jid: String) = runCatching {
        val response = api.blockContact(BlockRequest(jid))
        if (!response.success) error(response.error ?: "Failed to block contact")
    }

    suspend fun unblockContact(jid: String) = runCatching {
        val response = api.unblockContact(BlockRequest(jid))
        if (!response.success) error(response.error ?: "Failed to unblock contact")
    }

    suspend fun reportContact(jid: String) = runCatching {
        val response = api.reportContact(BlockRequest(jid))
        if (!response.success) error(response.error ?: "Failed to report contact")
    }

    suspend fun getGroupInfo(jid: String): Result<GroupInfo> = runCatching {
        api.getGroupInfo(jid)
    }

    suspend fun getCommonGroups(contactJid: String, allLocalContacts: List<Contact>): Result<List<Contact>> = runCatching {
        val userGroups = allLocalContacts.filter { it.isGroup }

        coroutineScope {
            userGroups.map { groupContact ->
                async {
                    getGroupInfo(groupContact.id)
                        .getOrNull()
                        ?.takeIf { groupInfo -> groupInfo.participants.any { it.id == contactJid } }
                        ?.let { groupContact }
                }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun getStatuses(allLocalContacts: List<Contact>): Result<List<StatusItem>> = runCatching {
        val contactsMap = allLocalContacts.associateBy { it.id }
        api.getStatuses().map { status ->
            status.copy(
                avatarUrl = ApiClient.resolveAvatarUrl(context, status.jid),
                senderName = contactsMap[status.jid]?.name ?: status.jid.substringBefore('@'),
                mediaUrl = ApiClient.resolveUrl(context, status.mediaUrl)
            )
        }
    }

    suspend fun sendStatus(file: File, caption: String?): Result<SendResponse> = runCatching {
        val tempId = UUID.randomUUID().toString()
        val response = api.sendStatus(
            caption = caption?.toTextRequestBody(),
            tempId = tempId.toTextRequestBody(),
            file = file.asFormDataPart("file")
        )
        if(response.success) response else error(response.error ?: "Send status failed")
    }
}
