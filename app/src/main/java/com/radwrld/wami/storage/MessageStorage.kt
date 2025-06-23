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
        return try {
            gson.fromJson(json, object : TypeToken<MutableList<Message>>() {}.type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveList(jid: String, list: List<Message>) {
        prefs.edit().putString(getKey(jid), gson.toJson(list)).apply()
    }

    
    fun getMessages(jid: String): MutableList<Message> = getList(jid)

    
    fun saveMessages(jid: String, messages: List<Message>) {

        val distinctSorted = messages.distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, distinctSorted)
    }
    
    
    fun appendMessages(jid: String, newMessages: List<Message>) {
        val existing = getList(jid)
        val combined = (existing + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, combined)
    }

    
    fun getLastMessage(jid: String): Message? {
        return getList(jid).maxByOrNull { it.timestamp }
    }

    
    fun addMessage(jid: String, message: Message) {
        val list = getList(jid)
        if (list.none { it.id == message.id }) {
            list.add(message)
            saveList(jid, list)
        }
    }

    
    fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) {
        update(jid, tempId) { it.copy(id = newId, status = newStatus) }
    }

    
    fun updateMessageStatus(jid: String, id: String, status: String) {
        update(jid, id) { it.copy(status = status) }
    }

    
    fun updateMessageLocalPath(jid: String, id: String, path: String) {
        update(jid, id) {
            if (it.localMediaPath != path) it.copy(localMediaPath = path) else it
        }
    }

    
    fun updateMessageReactions(jid: String, id: String, reactions: Map<String, Int>) {
        update(jid, id) { it.copy(reactions = reactions) }
    }

    private fun update(jid: String, id: String, transform: (Message) -> Message) {
        val list = getList(jid)
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = transform(list[index])
            saveList(jid, list)
        }
    }
}
