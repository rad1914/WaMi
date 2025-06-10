// app/src/main/java/com/radwrld/wami/model/Message.kt
package com.radwrld.wami.model

data class Message(
    val name: String,
    val text: String = "",
    var status: String = "",
    // CHANGED: 'val' is now 'var' to allow updating the ID
    var id: String = "",
    val jid: String = "",
    val isOutgoing: Boolean = false,
    val timestamp: Long = 0L,
    val lastMessage: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = "",
    val isOnline: Boolean = false
)
