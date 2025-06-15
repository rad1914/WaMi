// @path: app/src/main/java/com/radwrld/wami/storage/MessageStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Message // Ensure this import is correct

class MessageStorage(context: Context) {

    private val prefs = context.getSharedPreferences("message_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun getKey(jid: String) = "messages_$jid"

    fun getMessages(jid: String): MutableList<Message> {
        val key = getKey(jid)
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Message>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMessages(jid: String, messages: List<Message>) {
        val key = getKey(jid)
        val json = gson.toJson(messages)
        prefs.edit().putString(key, json).apply()
    }

    fun addMessage(jid: String, message: Message) {
        val messages = getMessages(jid)
        // Avoid adding duplicates
        if (messages.none { it.id == message.id }) {
            messages.add(message)
            saveMessages(jid, messages)
        }
    }

    // UPDATED: Use .copy() to create a new message instance instead of mutating the old one.
    fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == tempId }
        if (messageIndex != -1) {
            val originalMessage = messages[messageIndex]
            val updatedMessage = originalMessage.copy(id = newId, status = newStatus)
            messages[messageIndex] = updatedMessage // Replace the old message with the new one
            saveMessages(jid, messages)
        }
    }

    // UPDATED: Use .copy() here as well for immutability and safety.
    fun updateMessageStatus(jid: String, messageId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            val originalMessage = messages[messageIndex]
            val updatedMessage = originalMessage.copy(status = newStatus)
            messages[messageIndex] = updatedMessage // Replace the old message with the new one
            saveMessages(jid, messages)
        }
    }
}
