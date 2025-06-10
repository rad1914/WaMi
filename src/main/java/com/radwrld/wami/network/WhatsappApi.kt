package com.radwrld.wami.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WhatsAppApi {
    @GET("history/{jid}")
    suspend fun getHistory(@Path("jid") jid: String): List<MessageHistoryItem>

    @POST("send")
    suspend fun sendMessage(@Body request: SendRequest): SendResponse

    @GET("chats")
    suspend fun getChats(): List<Conversation>
}
