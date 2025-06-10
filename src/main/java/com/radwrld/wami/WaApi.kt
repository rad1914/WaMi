// /api/WaAPi.kt
package com.radwrld.wami

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// --- Models ---
data class ChatMessageDto(
  val id: Long,
  val jid: String,
  val text: String,
  @SerializedName("isOutgoing") val isOutgoing: Int,
  val status: String,
  val timestamp: Long
)

data class SendRequest(val jid: String, val text: String)
data class StatusResponse(val connected: Boolean)
data class QrResponse(val qr: String)

// --- Main API for chat activity (Coroutines) ---
interface WhatsAppApi {
  /**  
   * GET  http://22.ip.gl.ply.gg:18880/history/{jid}?limit=200  
   */
  @GET("history/{jid}")
  suspend fun getHistory(
    @Path("jid", encoded = true) jid: String,
    @Query("limit") limit: Int = 200
  ): List<ChatMessageDto>

  /**  
   * POST http://22.ip.gl.ply.gg:18880/send  
   * Body: { "jid":"...", "text":"..." }  
   */
  @Headers("Content-Type: application/json")
  @POST("send")
  suspend fun sendMessage(@Body req: SendRequest): String
}

// --- Alternative API (if you ever need Call<T>) ---
interface WaApi {
  @GET("status")
  suspend fun getStatus(): StatusResponse

  @GET("qrcode")
  suspend fun getQr(): QrResponse

  @Headers("Content-Type: application/json")
  @POST("send")
  suspend fun sendMessage(@Body req: SendRequest): String
}
