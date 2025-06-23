// @path: app/src/main/java/com/radwrld/wami/network/Models.kt
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
    val type: String? = "conversation",
    val status: String = "sending",
    val timestamp: Long = System.currentTimeMillis(),
    val name: String? = null,
    val senderName: String? = null,
    val mediaUrl: String? = null,
    val localMediaPath: String? = null,
    val mimetype: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val mediaSha256: String? = null
) {

    fun hasMedia(): Boolean = !mediaUrl.isNullOrBlank()
    fun isVideo(): Boolean = mimetype?.startsWith("video/") == true
    fun hasText(): Boolean = !text.isNullOrBlank()
    fun isSticker(): Boolean = type == "sticker"
}
