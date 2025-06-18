// @path: app/src/main/java/com/radwrld/wami/network/ApiModels.kt
package com.radwrld.wami.network

import com.google.gson.annotations.SerializedName

data class SendResponse(
    val success: Boolean,
    val messageId: String?,
    val tempId: String?,
    val timestamp: Long?,
    val error: String?
)

data class MessageHistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("jid") val jid: String,
    @SerializedName("text") val text: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("isOutgoing") val isOutgoing: Int,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("mimetype") val mimetype: String?,
    @SerializedName("quoted_message_id") val quotedMessageId: String?,
    @SerializedName("quoted_message_text") val quotedMessageText: String?,
    @SerializedName("reactions") val reactions: Map<String, Int>?
)

data class Conversation(
    @SerializedName("jid") val jid: String,
    @SerializedName("name") val name: String?,
    @SerializedName("is_group") val isGroupInt: Int = 0,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Long?,
    @SerializedName("unreadCount") val unreadCount: Int?
) {
    val isGroup: Boolean = isGroupInt == 1
}

data class SessionResponse(val sessionId: String)

data class StatusResponse(val connected: Boolean, val qr: String?)

data class SendMessageRequest(
    val jid: String,
    val text: String,
    val tempId: String
)

data class SendReactionRequest(
    val jid: String,
    val messageId: String,
    val fromMe: Boolean,
    val emoji: String
)

data class MessageStatusUpdateDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String
)
