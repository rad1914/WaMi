package com.radwrld.wami.data

import com.radwrld.wami.Constants
import okhttp3.*
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createSession(): String? {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/create")
            .post("".toRequestBody())
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body!!.string()
            return JSONObject(body).optString("sessionId", null)
        }
    }

    fun getStatus(sessionId: String): Pair<Boolean, String?> {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/status")
            .header("Authorization", "Bearer $sessionId")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false to null
            val json = JSONObject(resp.body!!.string())
            return json.getBoolean("connected") to json.optString("qr", null)
        }
    }

    fun logout(sessionId: String): Boolean {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/logout")
            .header("Authorization", "Bearer $sessionId")
            .post("".toRequestBody())
            .build()
        client.newCall(req).execute().use { return it.isSuccessful }
    }

    fun fetchChats(sessionId: String): List<Chat> {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/chats")
            .header("Authorization", "Bearer $sessionId")
            .build()
        client.newCall(req).execute().use { resp ->
            val arr = resp.body!!.string().let { org.json.JSONArray(it) }
            return List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Chat(o.getString("jid"), o.optString("name", o.getString("jid")))
            }
        }
    }

    fun fetchHistory(sessionId: String, jid: String): List<Message> {
        val enc = URLEncoder.encode(jid, "UTF-8")
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/history/$enc")
            .header("Authorization", "Bearer $sessionId")
            .build()
        client.newCall(req).execute().use { resp ->
            val arr = org.json.JSONArray(resp.body!!.string())
            return List(arr.length()) { i ->
                Message.fromJson(arr.getJSONObject(i))
            }
        }
    }

    fun sendText(sessionId: String, jid: String, text: String) {
        val json = JSONObject()
            .put("jid", jid)
            .put("text", text)
            .put("tempId", java.util.UUID.randomUUID().toString())
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/send")
            .header("Authorization", "Bearer $sessionId")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().close()
    }
}
