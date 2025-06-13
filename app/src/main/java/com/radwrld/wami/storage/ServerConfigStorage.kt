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
    private val KEY_IS_LOGGED_IN = "is_logged_in"
    // Key for storing the multi-user session token
    private val SESSION_ID_KEY = "session_id"

    init {
        if (!sharedPreferences.contains(PRIMARY_KEY)) {
            sharedPreferences.edit()
                .putString(PRIMARY_KEY, "22.ip.gl.ply.gg:18880")
                .putString(FALLBACK_KEY, "127.0.0.1:3007")
                .putInt(INDEX_KEY, 0)
                .apply()
        }
    }
    
    // --- Session ID Methods ---
    
    /**
     * Saves the session ID token to SharedPreferences.
     * @param sessionId The token to save, or null to clear it.
     */
    fun saveSessionId(sessionId: String?) {
        sharedPreferences.edit().putString(SESSION_ID_KEY, sessionId).apply()
        Log.d("ServerConfigStorage", "Session ID saved: $sessionId")
    }

    /**
     * Retrieves the session ID token from SharedPreferences.
     * @return The saved session ID, or null if it doesn't exist.
     */
    fun getSessionId(): String? {
        val sessionId = sharedPreferences.getString(SESSION_ID_KEY, null)
        Log.d("ServerConfigStorage", "Retrieved Session ID: $sessionId")
        return sessionId
    }


    // --- Existing Methods ---

    val primaryServer: String
        get() = sharedPreferences.getString(PRIMARY_KEY, "22.ip.gl.ply.gg:19071") ?: "22.ip.gl.ply.gg:19071"

    val fallbackServer: String
        get() = sharedPreferences.getString(FALLBACK_KEY, "127.0.0.1:3007") ?: "127.0.0.1:3007"

    private val customServer: String?
        get() = settingsPrefs.getString(SettingsActivity.CUSTOM_IP_KEY, null)

    fun isLoggedIn(): Boolean {
        val loggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d("ServerConfigStorage", "Checking login state: isLoggedIn=$loggedIn")
        return loggedIn
    }

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
            return getCurrentServer()
        }
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
