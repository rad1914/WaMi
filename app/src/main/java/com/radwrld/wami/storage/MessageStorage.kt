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

    private fun getList(jid: String): MutableList<Message> {
        val json = prefs.getString(getKey(jid), null) ?: return mutableListOf()
        return gson.fromJson(json, object : TypeToken<MutableList<Message>>() {}.type)
    }

    private fun saveList(jid: String, list: List<Message>) {
        prefs.edit().putString(getKey(jid), gson.toJson(list)).apply()
    }

    /** Public API to retrieve messages */
    fun getMessages(jid: String): MutableList<Message> = getList(jid)

    /** Restored public API for saving an entire list at once */
    fun saveMessages(jid: String, messages: List<Message>) {
        saveList(jid, messages)
    }

    /** Add a single message if not already present */
    fun addMessage(jid: String, message: Message) {
        val list = getList(jid)
        if (list.none { it.id == message.id }) {
            list.add(message)
            saveList(jid, list)
        }
    }

    /** Update a message's ID and status based on a temporary ID */
    fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) {
        update(jid, tempId) { it.copy(id = newId, status = newStatus) }
    }

    /** Update only the status of a message */
    fun updateMessageStatus(jid: String, id: String, status: String) {
        update(jid, id) { it.copy(status = status) }
    }

    /** Update only the local media path of a message */
    fun updateMessageLocalPath(jid: String, id: String, path: String) {
        update(jid, id) {
            if (it.localMediaPath != path) it.copy(localMediaPath = path) else it
        }
    }

    /**
     * Internal helper to find, transform, and save a single message.
     *
     * @param jid    the chat identifier
     * @param id     the message ID to look up
     * @param transform  a function that takes the existing Message and returns an updated copy
     */
    private fun update(jid: String, id: String, transform: (Message) -> Message) {
        val list = getList(jid)
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = transform(list[index])
            saveList(jid, list)
        }
    }
}
