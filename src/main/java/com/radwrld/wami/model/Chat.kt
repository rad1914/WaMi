// app/src/main/java/com/radwrld/wami/model/Chat.kt
package com.radwrld.wami.model

/**
 * Represents a chat conversation summary in the main list.
 *
 * @param contactName The name of the contact.
 * @param lastMessage The last message preview in the chat.
 * @param avatarUrl The URL for the contact's avatar.
 * @param phoneNumber The contact's phone number, used to construct the JID.
 * @param unreadCount The number of unread messages.
 */
data class Chat(
    val contactName: String,
    val lastMessage: String,
    val avatarUrl: String,
    val phoneNumber: String,
    val unreadCount: Int = 0
)
