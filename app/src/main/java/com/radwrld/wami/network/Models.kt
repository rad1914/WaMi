// @path: app/src/main/java/com/radwrld/wami/model/Models.kt
package com.radwrld.wami.model

import java.util.UUID

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val jid: String,
    val text: String?,
    val isOutgoing: Boolean,
    val type: String? = "conversation", // e.g., "conversation", "sticker", "image", "video"
    val status: String = "sending",
    val timestamp: Long = System.currentTimeMillis(),
    val name: String? = null, // Used for the sender of a quoted message
    val senderName: String? = null, // Used for the sender of an incoming message
    val mediaUrl: String? = null,
    val localMediaPath: String? = null,
    val mimetype: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null,
    val reactions: Map<String, Int> = emptyMap()
) {
    // Helper functions for use in XML data binding
    fun hasMedia(): Boolean = mediaUrl != null || localMediaPath != null
    fun isVideo(): Boolean = mimetype?.startsWith("video/") == true
    fun hasText(): Boolean = !text.isNullOrBlank()
    fun getMediaPath(): String? = localMediaPath ?: mediaUrl
    fun isSticker(): Boolean = type == "sticker"
}
