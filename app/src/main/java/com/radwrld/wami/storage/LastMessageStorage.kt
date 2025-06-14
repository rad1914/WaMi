// @path: app/src/main/java/com/radwrld/wami/storage/LastMessageStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.radwrld.wami.model.Message

class LastMessageStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("last_messages_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Saves the last message for a given JID.
     * @param jid The Jabber ID of the chat.
     * @param message The message object to save.
     */
    fun saveLastMessage(jid: String, message: Message) {
        val json = gson.toJson(message)
        prefs.edit().putString(jid, json).apply()
    }

    /**
     * Retrieves the last saved message for a given JID.
     * @param jid The Jabber ID of the chat.
     * @return The last saved Message object, or null if none exists.
     */
    fun getLastMessage(jid: String): Message? {
        val json = prefs.getString(jid, null) ?: return null
        return try {
            gson.fromJson(json, Message::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
