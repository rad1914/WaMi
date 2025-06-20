// @path: app/src/main/java/com/radwrld/wami/storage/ConversationStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Contact

class ConversationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("conversation_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CONVERSATIONS = "key_conversations"
    }

    fun saveConversations(conversations: List<Contact>) {
        val json = gson.toJson(conversations)
        prefs.edit().putString(KEY_CONVERSATIONS, json).apply()
    }

    fun getConversations(): List<Contact> {
        val json = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Contact>>() {}.type
        return gson.fromJson(json, type)
    }
}
