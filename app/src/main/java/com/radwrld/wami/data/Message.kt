// @path: app/src/main/java/com/radwrld/wami/data/Message.kt
package com.radwrld.wami.data

import org.json.JSONObject

data class Chat(val jid: String, val name: String)

data class Message(
    val id: String,
    val fromMe: Boolean,
    val text: String?,
    val timestamp: Long
) {
    companion object {
        fun fromJson(o: JSONObject): Message = Message(
            id        = o.getString("id"),
            fromMe    = o.getBoolean("isOutgoing"),
            text      = o.optString("text", null), 
            timestamp = o.optLong("timestamp")
        )
    }
}
