// @path: app/src/main/java/com/radwrld/wami/data/ApiService.kt
package com.radwrld.wami.data

import com.radwrld.wami.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun authReq(url: String, sessionId: String, method: String = "GET", body: RequestBody? = null) =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $sessionId")
            .method(method, body)
            .build()

    private fun emptyBody() = "".toRequestBody()
    private fun jsonBody(json: JSONObject) = json.toString().toRequestBody("application/json".toMediaType())

    fun createSession(): String? =
        client.newCall(Request.Builder().url("${Constants.BASE_URL}/create").post(emptyBody()).build())
            .execute().use { it.takeIf { r -> r.isSuccessful }?.body?.string()?.let { b -> JSONObject(b).optString("sessionId", null) } }

    fun getStatus(sessionId: String): Pair<Boolean, String?> =
        client.newCall(authReq("${Constants.BASE_URL}/status", sessionId)).execute().use {
            if (!it.isSuccessful) return false to null
            JSONObject(it.body!!.string()).let { j -> j.getBoolean("connected") to j.optString("qr", null) }
        }

    fun logout(sessionId: String): Boolean =
        client.newCall(authReq("${Constants.BASE_URL}/logout", sessionId, "POST", emptyBody()))
            .execute().use { it.isSuccessful }

    fun fetchChats(sessionId: String): List<Chat> =
        client.newCall(authReq("${Constants.BASE_URL}/chats", sessionId)).execute().use {
            JSONArray(it.body!!.string()).let { arr ->
                List(arr.length()) { i ->
                    arr.getJSONObject(i).let { o -> Chat(o.getString("jid"), o.optString("name", o.getString("jid"))) }
                }
            }
        }

    fun fetchHistory(sessionId: String, jid: String): List<Message> =
        client.newCall(authReq("${Constants.BASE_URL}/history/${URLEncoder.encode(jid, "UTF-8")}", sessionId)).execute().use {
            JSONArray(it.body!!.string()).let { arr ->
                List(arr.length()) { i -> Message.fromJson(arr.getJSONObject(i)) }
            }
        }

    fun sendText(sessionId: String, jid: String, text: String) {
        val json = JSONObject().put("jid", jid).put("text", text).put("tempId", UUID.randomUUID().toString())
        client.newCall(authReq("${Constants.BASE_URL}/send", sessionId, "POST", jsonBody(json))).execute().close()
    }

    fun sendReaction(sessionId: String, jid: String, messageId: String, emoji: String) {
        val json = JSONObject().put("jid", jid).put("messageId", messageId).put("emoji", emoji)
        client.newCall(authReq("${Constants.BASE_URL}/send/reaction", sessionId, "POST", jsonBody(json))).execute().close()
    }

    fun sendMedia(sessionId: String, jid: String, fileBytes: ByteArray, fileName: String, mimeType: String, caption: String? = null) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("jid", jid)
            .addFormDataPart("tempId", UUID.randomUUID().toString())
            .addFormDataPart("file", fileName, fileBytes.toRequestBody(mimeType.toMediaType()))
            .apply { caption?.let { addFormDataPart("caption", it) } }
            .build()

        client.newCall(authReq("${Constants.BASE_URL}/send/media", sessionId, "POST", body)).execute().close()
    }

    fun downloadMedia(sessionId: String, messageId: String): Response? =
        client.newCall(authReq("${Constants.BASE_URL}/media/$messageId", sessionId)).execute()
            .takeIf { it.isSuccessful }

    fun getAvatar(sessionId: String, jid: String): Response? =
        client.newCall(authReq("${Constants.BASE_URL}/avatar/${URLEncoder.encode(jid, "UTF-8")}", sessionId)).execute()
            .takeIf { it.isSuccessful }

    fun syncHistory(sessionId: String, jid: String): Boolean =
        client.newCall(authReq("${Constants.BASE_URL}/history/sync/${URLEncoder.encode(jid, "UTF-8")}", sessionId, "POST", emptyBody()))
            .execute().use { it.isSuccessful }
}
