package com.radwrld.wami.model

/**
 * Represents a single chat message, either for the contact list (MainActivity)
 * or for the chat screen (ChatActivity).
 *
 * @param name       Display name of the sender.
 * @param text       Message body.
 * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
 * @param id         Unique message identifier.
 * @param jid        Jabber ID (or other internal ID) of the sender.
 * @param isOutgoing True if this message was sent by the local user.
 * @param timestamp  Unix‐epoch milliseconds when this message was created.
 * @param lastMessage The last message for the contact list (MainActivity only).
 * @param avatarUrl  Avatar URL for the contact (MainActivity only).
 * @param phoneNumber The phone number of the sender (MainActivity only).
 * @param isOnline   Indicates if the contact is online (MainActivity only).
 */
data class Message(
    val name: String,
    val text: String = "",
    var status: String = "",
    val id: String = "",
    val jid: String = "",
    val isOutgoing: Boolean = false,
    val timestamp: Long = 0L,

    // Fields used only in MainActivity for contact list
    val lastMessage: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = "",
    val isOnline: Boolean = false
)
