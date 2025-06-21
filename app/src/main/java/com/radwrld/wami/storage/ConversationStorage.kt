// @path: app/src/main/java/com/radwrld/wami/storage/ConversationStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.model.Contact

class ConversationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("conv_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveConversations(conversations: List<Contact>) {
        prefs.edit()
            .putString("conversations", gson.toJson(conversations))
            .apply()
    }

    fun getConversations(): List<Contact> = prefs
        .getString("conversations", null)
        ?.let { json ->
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson<List<Contact>>(json, type)
        }
        ?: emptyList()
}