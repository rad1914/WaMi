package com.radwrld.wami.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/** --- Data Models for API Communication --- */
data class SessionResponse(val sessionId: String)
data class StatusResponse(val connected: Boolean, val qr: String?)

/** --- Unified API Interface --- */
interface WhatsAppApi {

    // Creates a new user session
    @POST("session/create")
    suspend fun createSession(): SessionResponse

    // Logs out the current user session
    @POST("session/logout")
    suspend fun logout(): Response<Void>

    // Get current status of the connection (now includes QR)
    @GET("status")
    suspend fun getStatus(): StatusResponse

    // Get list of chat conversations
    @GET("chats")
    suspend fun getConversations(): List<Conversation>

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

    // Export the current session data as a zip file
    @Streaming
    @GET("session/export")
    suspend fun exportSession(): Response<ResponseBody>
}
