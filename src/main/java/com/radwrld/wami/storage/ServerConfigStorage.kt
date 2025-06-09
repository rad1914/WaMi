// ServerConfigStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences

class ServerConfigStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("server_config_pref", Context.MODE_PRIVATE)

    private val PRIMARY_KEY = "server_primary"
    private val FALLBACK_KEY = "server_fallback"
    private val INDEX_KEY = "server_index"

    init {
        if (!sharedPreferences.contains(PRIMARY_KEY)) {
            sharedPreferences.edit()
                .putString(PRIMARY_KEY, "22.ip.gl.ply.gg:18880")  
                .putString(FALLBACK_KEY, "127.0.0.1:3007")       
                .putInt(INDEX_KEY, 0)
                .apply()
        }
    }

    val primaryServer: String
        get() = sharedPreferences.getString(PRIMARY_KEY, "22.ip.gl.ply.gg:19071")!!

    val fallbackServer: String
        get() = sharedPreferences.getString(FALLBACK_KEY, "127.0.0.1:3007")!!

    fun saveServers(primary: String, fallback: String) {
        sharedPreferences.edit()
            .putString(PRIMARY_KEY, primary)
            .putString(FALLBACK_KEY, fallback)
            .putInt(INDEX_KEY, 0)
            .apply()
    }

    fun getCurrentServer(): String {
        val idx = sharedPreferences.getInt(INDEX_KEY, 0)
        return if (idx == 0)
            primaryServer
        else
            fallbackServer
    }

    fun moveToNextServer(): String {
        val nextIdx = 1 - sharedPreferences.getInt(INDEX_KEY, 0)
        sharedPreferences.edit().putInt(INDEX_KEY, nextIdx).apply()
        return getCurrentServer()
    }

    fun resetToPrimary() {
        sharedPreferences.edit().putInt(INDEX_KEY, 0).apply()
    }
}
