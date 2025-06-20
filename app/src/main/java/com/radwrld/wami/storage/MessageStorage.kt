// @path: app/src/main/java/com/radwrld/wami/storage/MessageStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Message

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
        if (messages.none { it.id == message.id }) {
            messages.add(message)
            saveMessages(jid, messages)
        }
    }

    fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == tempId }
        if (messageIndex != -1) {
            val originalMessage = messages[messageIndex]
            val updatedMessage = originalMessage.copy(id = newId, status = newStatus)
            messages[messageIndex] = updatedMessage
            saveMessages(jid, messages)
        }
    }

    fun updateMessageStatus(jid: String, messageId: String, newStatus: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            val originalMessage = messages[messageIndex]
            val updatedMessage = originalMessage.copy(status = newStatus)
            messages[messageIndex] = updatedMessage
            saveMessages(jid, messages)
        }
    }

    // ++ New method to update a message with its local file path after a successful download.
    fun updateMessageLocalPath(jid: String, messageId: String, localPath: String) {
        val messages = getMessages(jid)
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            val originalMessage = messages[messageIndex]
            // Only update if the path is not already set
            if (originalMessage.localMediaPath != localPath) {
                val updatedMessage = originalMessage.copy(localMediaPath = localPath)
                messages[messageIndex] = updatedMessage
                saveMessages(jid, messages)
            }
        }
    }
}
