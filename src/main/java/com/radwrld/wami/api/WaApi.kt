// /api/WaApi.kt

package com.radwrld.wami.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class StatusResponse(val connected: Boolean)
data class QrResponse(val qr: String)
data class SendMessageRequest(val jid: String, val text: String)

interface WaApi {
    @GET("status")
    fun getStatus(): Call<StatusResponse>

    @GET("qrcode")
    fun getQr(): Call<QrResponse>

    @POST("send")
    fun sendMessage(@Body req: SendMessageRequest): Call<Void>
}
