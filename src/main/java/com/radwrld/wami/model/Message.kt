// app/src/main/java/com/radwrld/wami/model/Message.kt
package com.radwrld.wami.model

data class Message(
    val name: String,
    val lastMessage: String,
    val avatarUrl: String,
    val phoneNumber: String,  // Store the contact number
    val isOnline: Boolean,
    val isOutgoing: Boolean = false // Default value for outgoing flag
)
