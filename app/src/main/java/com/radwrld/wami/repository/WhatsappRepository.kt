package com.radwrld.wami.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.SendResponse
import com.radwrld.wami.storage.MessageStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder

/**
 * Single source of truth for WhatsApp data.
 */
class WhatsAppRepository(private val context: Context) {

    private val api = ApiClient.getInstance(context)
    private val messageStorage = MessageStorage(context)

    suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            Result.success(api.getConversations())
        } catch (e: Exception) {
            Log.e("WhatsAppRepo", "Failed to fetch conversations", e)
            Result.failure(e)
        }
    }

    suspend fun getMessageHistory(jid: String): Result<List<Message>> {
        return try {
            val jidEncoded = URLEncoder.encode(jid, "UTF-8")
            val remoteMessagesDto = api.getHistory(jidEncoded)
            
            val remoteMessages = remoteMessagesDto.map { dto ->
                Message(
                    id = dto.id,
                    jid = dto.jid,
                    text = dto.text,
                    isOutgoing = dto.isOutgoing > 0,
                    status = dto.status,
                    timestamp = dto.timestamp,
                    mediaUrl = dto.mediaUrl,
                    mimetype = dto.mimetype,
                    quotedMessageId = dto.quotedMessageId,
                    quotedMessageText = dto.quotedMessageText
                )
            }
            
            messageStorage.saveMessages(jid, remoteMessages)
            Result.success(remoteMessages)
        } catch (e: Exception) {
            Log.e("WhatsAppRepo", "Failed to fetch message history for $jid", e)
            Result.failure(e)
        }
    }

    /**
     * UPDATED: This function now returns Result<SendResponse> to pass the full server
     * response back to the ViewModel, including the final messageId.
     */
    suspend fun sendTextMessage(jid: String, text: String, tempId: String): Result<SendResponse> {
        return try {
            val request = SendMessageRequest(jid, text, tempId)
            val response = api.sendMessage(request)
            if (response.success) {
                Result.success(response)
            } else {
                throw Exception(response.error ?: "Unknown error sending message")
            }
        } catch (e: Exception) {
            Log.e("WhatsAppRepo", "Failed to send message", e)
            Result.failure(e)
        }
    }

    suspend fun sendMediaMessage(jid: String, fileUri: Uri, caption: String?): Result<Unit> {
        return try {
            val jidPart = jid.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionPart = caption?.toRequestBody("text/plain".toMediaTypeOrNull())

            val file = createTempFileFromUri(fileUri)
            val fileMimeType = context.contentResolver.getType(fileUri)
            val requestFile = file.asRequestBody(fileMimeType?.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.sendMedia(jidPart, captionPart, filePart)
            file.delete()

            if (response.success) {
                Result.success(Unit)
            } else {
                throw Exception(response.error ?: "Unknown error sending media")
            }
        } catch (e: Exception) {
            Log.e("WhatsAppRepo", "Failed to send media", e)
            Result.failure(e)
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open input stream for URI: $uri")
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream.use { it.copyTo(fileOut) }
        }
        return tempFile
    }
}
