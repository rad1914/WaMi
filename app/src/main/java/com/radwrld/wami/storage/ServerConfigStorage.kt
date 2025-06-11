// @path: app/src/main/java/com/radwrld/wami/storage/ServerConfigStorage.kt

package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.radwrld.wami.SettingsActivity

class ServerConfigStorage(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("server_config_pref", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)


    private val PRIMARY_KEY = "server_primary"
    private val FALLBACK_KEY = "server_fallback"
    private val INDEX_KEY = "server_index"
    // **APPLIED: Key for storing login state**
    private val KEY_IS_LOGGED_IN = "is_logged_in"

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
        get() = sharedPreferences.getString(PRIMARY_KEY, "22.ip.gl.ply.gg:19071") ?: "22.ip.gl.ply.gg:19071"

    val fallbackServer: String
        get() = sharedPreferences.getString(FALLBACK_KEY, "127.0.0.1:3007") ?: "127.0.0.1:3007"

    private val customServer: String?
        get() = settingsPrefs.getString(SettingsActivity.CUSTOM_IP_KEY, null)

    // **APPLIED: Method to check if the user is logged in**
    fun isLoggedIn(): Boolean {
        val loggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d("ServerConfigStorage", "Checking login state: isLoggedIn=$loggedIn")
        return loggedIn
    }

    // **APPLIED: Method to save the user's login state**
    fun saveLoginState(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
        Log.d("ServerConfigStorage", "Login state saved: isLoggedIn=$isLoggedIn")
    }

    fun saveServers(primary: String, fallback: String) {
        sharedPreferences.edit()
            .putString(PRIMARY_KEY, primary)
            .putString(FALLBACK_KEY, fallback)
            .putInt(INDEX_KEY, 0)
            .apply()
        Log.d("ServerConfigStorage", "Servers saved: primary=$primary, fallback=$fallback")
    }

    fun getCurrentServer(): String {
        val useCustomIP = settingsPrefs.getBoolean(SettingsActivity.ENABLE_CUSTOM_IP_KEY, false)
        if (useCustomIP) {
            customServer?.let {
                if (it.isNotBlank()) {
                    Log.d("ServerConfigStorage", "Using custom server: $it")
                    return it
                }
            }
        }

        val idx = sharedPreferences.getInt(INDEX_KEY, 0)
        val server = if (idx == 0) primaryServer else fallbackServer
        Log.d("ServerConfigStorage", "Current server: $server (Index: $idx)")
        return server
    }

    fun moveToNextServer(): String {
        val useCustomIP = settingsPrefs.getBoolean(SettingsActivity.ENABLE_CUSTOM_IP_KEY, false)
        if (useCustomIP) {
            // If custom IP is enabled, there's no next server to move to.
            // We stick with the custom one.
            return getCurrentServer()
        }
        // Switch between primary and fallback servers
        val nextIdx = 1 - sharedPreferences.getInt(INDEX_KEY, 0)
        sharedPreferences.edit().putInt(INDEX_KEY, nextIdx).apply()
        val nextServer = getCurrentServer()
        Log.d("ServerConfigStorage", "Moved to next server: $nextServer (Index: $nextIdx)")
        return nextServer
    }

    fun resetToPrimary() {
        sharedPreferences.edit().putInt(INDEX_KEY, 0).apply()
        Log.d("ServerConfigStorage", "Reset to primary server")
    }
}
