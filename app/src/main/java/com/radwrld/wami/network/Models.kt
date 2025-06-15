// @path: app/src/main/java/com/radwrld/wami/network/Models.kt
package com.radwrld.wami.model

import java.util.UUID

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null
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
    val mimetype: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null
)
