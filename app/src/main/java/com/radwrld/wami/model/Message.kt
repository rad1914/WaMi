// @path: app/src/main/java/com/radwrld/wami/model/Message.kt
package com.radwrld.wami.model

data class Message(
    val name: String,
    val text: String = "",
    var status: String = "",
    var id: String = "",
    val jid: String = "",
    val isOutgoing: Boolean = false,
    val timestamp: Long = 0L,
    val lastMessage: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = "",
    val isOnline: Boolean = false,
    // Add this field to hold the name of the sender in a group
    val senderName: String? = null
)
