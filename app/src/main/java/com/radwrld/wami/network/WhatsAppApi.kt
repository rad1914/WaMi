// @path: app/src/main/java/com/radwrld/wami/network/WhatsappApi.kt
package com.radwrld.wami.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface WhatsAppApi {
    @GET("history/{jid}")
    suspend fun getHistory(
        @Path("jid", encoded = true) jid: String,
        @Query("limit") limit: Int = 200
    ): List<MessageHistoryItem>

    @POST("send")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendResponse

    @Multipart
    @POST("send/media")
    suspend fun sendMedia(
        @Part("jid") jid: RequestBody,
        @Part("caption") caption: RequestBody?,
        @Part file: MultipartBody.Part
    ): SendResponse

    @POST("send/reaction")
    suspend fun sendReaction(@Body request: SendReactionRequest): Response<Void>

    @GET("status")
    suspend fun getStatus(): StatusResponse

    @POST("session/create")
    suspend fun createSession(): SessionResponse

    @POST("session/logout")
    suspend fun logout(): Response<Void>

    @GET("chats")
    suspend fun getConversations(): List<Conversation>

    @Streaming
    @GET("session/export")
    suspend fun exportSession(): Response<ResponseBody>

    @Multipart
    @POST("session/import")
    suspend fun importSession(@Part file: MultipartBody.Part): Response<Void>
}
