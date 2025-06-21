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

    /** Public API to retrieve messages */
    fun getMessages(jid: String): MutableList<Message> = getList(jid)

    /** Restored public API for saving an entire list at once */
    fun saveMessages(jid: String, messages: List<Message>) {
        // Guardar la lista combinada y sin duplicados, ordenada por tiempo
        val distinctSorted = messages.distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, distinctSorted)
    }
    
    /** ++ NUEVA: Agrega mensajes más antiguos a la lista existente (para paginación) */
    fun appendMessages(jid: String, newMessages: List<Message>) {
        val existing = getList(jid)
        val combined = (existing + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, combined)
    }

    /** ++ NUEVA: Obtiene el último mensaje de un chat */
    fun getLastMessage(jid: String): Message? {
        return getList(jid).maxByOrNull { it.timestamp }
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

    private fun update(jid: String, id: String, transform: (Message) -> Message) {
        val list = getList(jid)
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = transform(list[index])
            saveList(jid, list)
        }
    }
}
