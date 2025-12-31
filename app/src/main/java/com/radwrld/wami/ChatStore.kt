package com.radwrld.wami

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object ChatStore {
    private const val KEY = "msgs"
    private lateinit var prefs: SharedPreferences
    @Volatile private var cached = emptyList<Message>()

    fun init(c: Context) {
        prefs = c.getSharedPreferences("chat", Context.MODE_PRIVATE)
        cached = prefs.getString(KEY, null)?.let {
            runCatching { Json.decodeFromString<List<Message>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    fun update(n: List<Message>) {
        cached = n
        prefs.edit().putString(KEY, Json.encodeToString(n)).apply()
    }

    fun get() = cached
}
