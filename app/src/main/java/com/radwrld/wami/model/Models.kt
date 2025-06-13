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
 *
 * UPDATED: Includes fields for media (images, videos) and replies (quoted messages).
 * All properties are 'val' to make this an immutable data class.
 * To update a message (e.g., changing its status), create a new instance
 * using the .copy() method:
 *
 * val updatedMessage = originalMessage.copy(status = "sent")
 *
 */
data class Message(
    val id: String,
    val jid: String,
    val name: String, // Name of the contact/group
    val text: String?, // Nullable to allow for media-only messages
    val status: String,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val senderName: String? = null, // For showing the sender's name in a group chat

    // --- ADDED: Fields for media and replies ---
    val mediaUrl: String? = null,
    val mimetype: String? = null,
    val quotedMessageId: String? = null,
    val quotedMessageText: String? = null
)
