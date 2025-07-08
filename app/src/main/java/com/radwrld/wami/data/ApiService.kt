// @path: app/src/main/java/com/radwrld/wami/data/ApiService.kt
package com.radwrld.wami.data

import com.radwrld.wami.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun emptyBody() = "".toRequestBody()
    private fun jsonBody(json: JSONObject) =
        json.toString().toRequestBody("application/json".toMediaType())

    private fun authReq(path: String, sessionId: String, method: String = "GET", body: RequestBody? = null) =
        Request.Builder()
            .url("${Constants.BASE_URL}/session$path")
            .header("Authorization", "Bearer $sessionId")
            .method(method, body)
            .build()

    fun createSession(): String? =
        client.newCall(
            Request.Builder()
                .url("${Constants.BASE_URL}/session/create")
                .post(emptyBody())
                .build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) return null
            JSONObject(resp.body!!.string()).optString("sessionId", null)
        }

    fun getStatus(sessionId: String): Pair<Boolean, String?> =
        client.newCall(authReq("/status", sessionId)).execute().use { resp ->
            if (!resp.isSuccessful) return false to null
            with(JSONObject(resp.body!!.string())) {
                getBoolean("connected") to optString("qr", null)
            }
        }

    fun logout(sessionId: String): Boolean =
        client.newCall(authReq("/logout", sessionId, "POST", emptyBody()))
            .execute().use { it.isSuccessful }

    fun fetchChats(sessionId: String): List<Chat> =
        client.newCall(authReq("/chats", sessionId)).execute().use { resp ->
            JSONArray(resp.body!!.string()).let { arr ->
                List(arr.length()) { i ->
                    arr.getJSONObject(i).let { Chat(it.getString("jid"), it.optString("name", it.getString("jid"))) }
                }
            }
        }

    fun fetchHistory(sessionId: String, jid: String): List<Message> =
        client.newCall(authReq("/history/${URLEncoder.encode(jid, "UTF-8")}", sessionId))
            .execute().use { resp ->
                JSONArray(resp.body!!.string()).let { arr ->
                    List(arr.length()) { i -> Message.fromJson(arr.getJSONObject(i)) }
                }
            }

    fun sendText(sessionId: String, jid: String, text: String) {
        val json = JSONObject()
            .put("jid", jid)
            .put("text", text)
            .put("tempId", UUID.randomUUID().toString())

        client.newCall(authReq("/send", sessionId, "POST", jsonBody(json))).execute().close()
    }

    fun sendReaction(sessionId: String, jid: String, messageId: String, emoji: String) {
        val json = JSONObject()
            .put("jid", jid)
            .put("messageId", messageId)
            .put("emoji", emoji)

        client.newCall(authReq("/send/reaction", sessionId, "POST", jsonBody(json))).execute().close()
    }

    fun sendMedia(
        sessionId: String,
        jid: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String? = null
    ) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("jid", jid)
            .addFormDataPart("tempId", UUID.randomUUID().toString())
            .addFormDataPart("file", fileName, fileBytes.toRequestBody(mimeType.toMediaType()))
            .apply { caption?.let { addFormDataPart("caption", it) } }
            .build()

        client.newCall(authReq("/send/media", sessionId, "POST", multipart)).execute().close()
    }

    fun downloadMedia(sessionId: String, messageId: String): Response? =
        client.newCall(authReq("/media/$messageId", sessionId))
            .execute().takeIf { it.isSuccessful }

    fun getAvatar(sessionId: String, jid: String): Response? =
        client.newCall(authReq("/avatar/${URLEncoder.encode(jid, "UTF-8")}", sessionId))
            .execute().takeIf { it.isSuccessful }

    fun syncHistory(sessionId: String, jid: String): Boolean =
        client.newCall(authReq("/history/sync/${URLEncoder.encode(jid, "UTF-8")}", sessionId, "POST", emptyBody()))
            .execute().use { it.isSuccessful }
}
