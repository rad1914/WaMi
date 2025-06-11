// @path: app/src/main/java/com/radwrld/wami/model/Models.kt
package com.radwrld.wami.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a chat conversation as returned by the server's /chats endpoint.
 * The field names are mapped to the JSON keys from the API to ensure correct parsing.
 */
data class Chat(
    @SerializedName("jid")
    val jid: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("last_message")
    val lastMessage: String?,

    @SerializedName("last_message_timestamp")
    val lastMessageTimestamp: Long? // Note: Made this nullable to prevent crashes if the API ever sends a null value.
)

/**
 * Represents a contact object used within the UI and local storage.
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null // Defaulting to null makes this optional
)
