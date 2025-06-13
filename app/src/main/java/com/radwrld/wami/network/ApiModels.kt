// @path: app/src/main/java/com/radwrld/wami/network/ApiModels.kt
package com.radwrld.wami.network

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the POST /send endpoint.
 */
data class SendResponse(
    val success: Boolean,
    val messageId: String?,
    val tempId: String?,
    val timestamp: Long?,
    val error: String?
)

/**
 * Represents a single message item from the GET /history/{jid} endpoint.
 */
data class MessageHistoryItem(
    val id: String,
    val jid: String,
    val text: String,
    val isOutgoing: Int,
    val status: String,
    val timestamp: Long
)

/**
 * Represents a single conversation item from the GET /chats endpoint.
 * This is the definitive model for your API.
 */
data class Conversation(
    @SerializedName("jid")
    val jid: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("last_message")
    val lastMessage: String?,

    @SerializedName("last_message_timestamp")
    val lastMessageTimestamp: Long?,

    // This field is required for the unread count feature.
    // Ensure your server API includes this field in the response.
    @SerializedName("unreadCount")
    val unreadCount: Int?
)

