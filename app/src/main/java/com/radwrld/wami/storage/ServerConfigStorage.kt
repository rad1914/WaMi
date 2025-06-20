// @path: app/src/main/java/com/radwrld/wami/storage/ServerConfigStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences
import com.radwrld.wami.SettingsActivity

class ServerConfigStorage(context: Context) {
    private val prefs = context.getSharedPreferences("server_config_pref", Context.MODE_PRIVATE)
    private val settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (!prefs.contains("server_primary")) {
            prefs.edit().putString("server_primary", "22.ip.gl.ply.gg:18880").apply()
        }
    }

    private fun formatUrl(url: String?): String {
        return url?.trim()?.let {
            (if (!it.startsWith("http")) "http://" else "") + it + if (!it.endsWith("/")) "/" else ""
        } ?: ""
    }

    fun saveSessionId(id: String?) = prefs.edit().putString("session_id", id).apply()
    fun getSessionId(): String? = prefs.getString("session_id", null)

    fun saveLoginState(state: Boolean) = prefs.edit().putBoolean("is_logged_in", state).apply()
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun savePrimaryServer(url: String) = prefs.edit().putString("server_primary", url).apply()

    fun getCurrentServer(): String {
        if (settings.getBoolean(SettingsActivity.ENABLE_CUSTOM_IP_KEY, false)) {
            val custom = formatUrl(settings.getString(SettingsActivity.CUSTOM_IP_KEY, null))
            if (custom.isNotBlank()) return custom
        }
        return formatUrl(prefs.getString("server_primary", ""))
    }
}
