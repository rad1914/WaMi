// @path: app/src/main/java/com/radwrld/wami/ChatStore.kt
package com.radwrld.wami

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ChatStore {
    private const val KEY = "msgs"
    private var prefs: SharedPreferences? = null
    
    @Volatile 
    private var cached = emptyList<Message>()

    fun init(c: Context) {
        prefs = c.getSharedPreferences("chat", Context.MODE_PRIVATE)
        prefs?.getString(KEY, null)?.let { jsonString ->
            cached = runCatching { 
                Json.decodeFromString<List<Message>>(jsonString) 
            }.getOrDefault(emptyList())
        }
    }

    fun update(n: List<Message>) {
        cached = n
        
        prefs?.edit()?.putString(KEY, Json.encodeToString(n))?.apply()
    }

    fun get() = cached
}