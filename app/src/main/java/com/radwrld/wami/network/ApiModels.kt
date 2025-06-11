// @path: app/src/main/java/com/radwrld/wami/network/ApiModels.kt
package com.radwrld.wami.network

import com.google.gson.annotations.SerializedName

data class SendRequest(
    val jid: String,
    val text: String,
    val tempId: String
)

data class SendResponse(
    val success: Boolean,
    val messageId: String?,
    val tempId: String?,
    val timestamp: Long?,
    val error: String?
)

data class MessageHistoryItem(
    val id: String,
    val jid: String,
    val text: String,
    val isOutgoing: Int,
    val status: String,
    val timestamp: Long
)

data class Conversation(
    val jid: String,
    val name: String?,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Long?
)
