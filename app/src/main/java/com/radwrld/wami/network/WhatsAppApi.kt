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
        @Query("before") before: Long,
        @Query("limit") limit: Int = 50
    ): List<MessageHistoryItem>
    
    @POST("history/sync/{jid}")
    suspend fun syncHistory(@Path("jid", encoded = true) jid: String): SyncResponse

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    @POST("send")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendResponse

    @Multipart
    @POST("send/media")
    suspend fun sendMedia(
        @Part("jid") jid: RequestBody,
        @Part("caption") caption: RequestBody?,
        @Part("tempId") tempId: RequestBody,
        @Part file: MultipartBody.Part
    ): SendResponse

    @POST("send/reaction")
    suspend fun sendReaction(@Body request: SendReactionRequest): Response<Void>

    @GET("session/status")
    suspend fun getStatus(): StatusResponse

    @POST("session/create")
    suspend fun createSession(): SessionResponse

    @POST("session/logout")
    suspend fun logout(): Response<Void>

    @GET("chats")
    suspend fun getConversations(): List<Conversation>

    // --- ENDPOINTS DE IMPORTAR/EXPORTAR ELIMINADOS ---
}
