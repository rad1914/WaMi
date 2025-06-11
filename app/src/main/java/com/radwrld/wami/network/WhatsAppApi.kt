package com.radwrld.wami.network

import retrofit2.Response
import retrofit2.http.*

/** --- Data Models not defined in ApiModels.kt --- */
// These were kept because they are used by the interface but were not in the ApiModels.kt file.
data class StatusResponse(val connected: Boolean)
data class QrResponse(val qr: String)


/** --- Unified API Interface --- */
interface WhatsAppApi {

    // Fetch message history with optional limit
    @GET("history/{jid}")
    suspend fun getHistory(
        @Path("jid", encoded = true) jid: String,
        @Query("limit") limit: Int = 200
    ): List<MessageHistoryItem>

    // Send a message
    @FormUrlEncoded
    @POST("send")
    suspend fun sendMessage(
        @Field("jid") jid: String,
        @Field("text") text: String,
        @Field("tempId") tempId: String
    ): SendResponse

    // Get current status of the connection
    @GET("status")
    suspend fun getStatus(): StatusResponse

    // Get current QR code for login
    @GET("qrcode")
    suspend fun getQr(): QrResponse

    // Get list of chat conversations
    @GET("chats")
    suspend fun getConversations(): List<Conversation>
}
