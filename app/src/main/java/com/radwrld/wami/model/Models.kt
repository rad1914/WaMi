// @path: app/src/main/java/com/radwrld/wami/model/Models.kt
package com.radwrld.wami.model

/**
 * Represents a contact/conversation object used within the UI (e.g., in MainActivity's list).
 * This class is mapped from the network 'Conversation' model.
 */
data class Contact(
    val id: String, // The JID from the server
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null,
    val lastMessage: String? = "Tap to start chatting", // To show in the list
    val lastMessageTimestamp: Long? = 0L, // For sorting or display
    val unreadCount: Int = 0 // For the notification dot
)

/**
 * Represents a single message object used within the ChatActivity UI.
 */
data class Message(
    var id: String, // ✅ CORRECTED: This must be 'var' to be changed later.
    val jid: String,
    val name: String, // Name of the contact/group
    val text: String = "",
    var status: String = "",
    val isOutgoing: Boolean = false,
    val timestamp: Long = 0L,
    val senderName: String? = null // For showing the sender's name in a group chat
)
