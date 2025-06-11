// @path: app/src/main/java/com/radwrld/wami/network/ApiService.kt
package com.radwrld.wami.network

import com.radwrld.wami.model.Chat
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    // Defines the GET request to the '/chats' endpoint
    @GET("chats")
    suspend fun getChats(): Response<List<Chat>>
}
