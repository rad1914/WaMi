// @path: app/src/main/java/com/radwrld/wami/network/ApiModels.kt
// @path: app/src/main/java/com/radwrld/wami/network/ApiModels.kt
package com.radwrld.wami.network

import com.google.gson.annotations.SerializedName

// --- Request/Response Models ---

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
    @SerializedName("isOutgoing") val isOutgoing: Int,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("mimetype") val mimetype: String?,
    @SerializedName("quoted_message_id") val quotedMessageId: String?,
    @SerializedName("quoted_message_text") val quotedMessageText: String?
)

// MODIFIED: Added avatarUrl to align with the updated backend response.
data class Conversation(
    @SerializedName("jid") val jid: String,
    @SerializedName("name") val name: String?,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Long?,
    @SerializedName("unreadCount") val unreadCount: Int?,
    @SerializedName("avatarUrl") val avatarUrl: String? // ADDED
)

// --- Session & Status Models ---

data class SessionResponse(val sessionId: String)

data class StatusResponse(val connected: Boolean, val qr: String?)

// --- Request Body Models ---

data class SendMessageRequest(
    val jid: String,
    val text: String,
    val tempId: String
)
