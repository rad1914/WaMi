// @path: app/src/main/java/com/radwrld/wami/storage/MessageStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Message

class MessageStorage(context: Context) {

    private val prefs = context.getSharedPreferences("message_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    // The key under which messages for a specific JID are stored.
    private fun getKey(jid: String) = "messages_$jid"

    /**
     * Retrieves all messages for a given JID from local storage.
     * @param jid The JID of the chat.
     * @return A mutable list of messages, or an empty list if none are found.
     */
    fun getMessages(jid: String): MutableList<Message> {
        val key = getKey(jid)
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Message>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Saves a complete list of messages for a JID, overwriting any existing data.
     * This is useful for synchronizing with the server's history.
     * @param jid The JID of the chat.
     * @param messages The list of messages to save.
     */
    fun saveMessages(jid: String, messages: List<Message>) {
        val key = getKey(jid)
        val json = gson.toJson(messages)
        prefs.edit().putString(key, json).apply()
    }

    /**
     * Adds a single message to the existing list for a JID.
     * @param jid The JID of the chat.
     * @param message The message to add.
     */
    fun addMessage(jid: String, message: Message) {
        val messages = getMessages(jid)
        messages.add(message)
        saveMessages(jid, messages)
    }

    /**
     * Updates a message's ID and status in the local storage.
     * Useful after a message is successfully sent to the server.
     * @param jid The JID of the chat.
     * @param tempId The temporary ID used when the message was created.
     * @param newId The final ID received from the server.
     * @param newStatus The new status of the message (e.g., "sent", "failed").
     */
    fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == tempId }
        if (messageIndex != -1) {
            messages[messageIndex].id = newId
            messages[messageIndex].status = newStatus
            saveMessages(jid, messages)
        }
    }
    
    /**
     * Updates the status of a message.
     * Useful for incoming status updates (e.g., "delivered", "read").
     * @param jid The JID of the chat associated with the message.
     * @param messageId The ID of the message to update.
     * @param newStatus The new status.
     */
    fun updateMessageStatus(jid: String, messageId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            messages[messageIndex].status = newStatus
            saveMessages(jid, messages)
        }
    }
}
