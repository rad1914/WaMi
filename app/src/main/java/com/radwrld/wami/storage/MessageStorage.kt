// @path: app/src/main/java/com/radwrld/wami/storage/MessageStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.network.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MessageStorage(context: Context) {

    private val prefs = context.getSharedPreferences("message_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _messageCache = MutableStateFlow<Map<String, List<Message>>>(loadEntireCache())
    
    val lastMessagesMapFlow: Flow<Map<String, Message?>> = _messageCache.map { cache ->
        cache.mapValues { (_, messages) -> messages.maxByOrNull { it.timestamp } }
    }

    private fun getKey(jid: String) = "messages_$jid"

    private fun loadEntireCache(): Map<String, List<Message>> {
        val allPrefs = prefs.all ?: return emptyMap()
        val messageMap = mutableMapOf<String, List<Message>>()
        val messageType = object : TypeToken<List<Message>>() {}.type

        for ((key, value) in allPrefs) {
            if (key.startsWith("messages_") && value is String) {
                try {
                    val jid = key.removePrefix("messages_")
                    val messages: List<Message> = gson.fromJson(value, messageType)
                    messageMap[jid] = messages
                } catch (e: Exception) {
                    // Ignorar entradas corruptas
                }
            }
        }
        return messageMap
    }

    private fun getList(jid: String): List<Message> {
        return _messageCache.value[jid] ?: emptyList()
    }

    private fun saveList(jid: String, list: List<Message>) {
        prefs.edit().putString(getKey(jid), gson.toJson(list)).apply()
        val currentCache = _messageCache.value.toMutableMap()
        currentCache[jid] = list
        _messageCache.value = currentCache
    }

    fun getMessages(jid: String): List<Message> = getList(jid)

    // FUNCIÓN AÑADIDA: Expone los mensajes como un Flow reactivo para el ChatViewModel.
    fun getMessagesFlow(jid: String): Flow<List<Message>> {
        return _messageCache.map { cache -> cache[jid] ?: emptyList() }
    }

    fun saveMessages(jid: String, messages: List<Message>) {
        val distinctSorted = messages.distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, distinctSorted)
    }
    
    fun appendMessages(jid: String, newMessages: List<Message>) {
        val existing = getList(jid)
        val combined = (existing + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
        saveList(jid, combined)
    }
    
    fun addMessage(jid: String, message: Message) {
        val list = getList(jid).toMutableList()
        val existingIndex = list.indexOfFirst { it.id == message.id }
        if (existingIndex != -1) {
             list[existingIndex] = message
        } else {
             list.add(message)
        }
        saveList(jid, list.sortedBy { it.timestamp })
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
        val list = getList(jid).toMutableList()
        val index = list.indexOfFirst { it.id == id }
         if (index != -1) {
            list[index] = transform(list[index])
            saveList(jid, list)
        }
    }
}