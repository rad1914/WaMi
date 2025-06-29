// @path: app/src/main/java/com/radwrld/wami/SettingsActivity.kt
package com.radwrld.wami

import android.app.KeyguardManager
import android.content.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.ui.screens.CustomIpDialog
import com.radwrld.wami.ui.screens.SettingsScreen
import com.radwrld.wami.ui.screens.ThemeDialog
import com.radwrld.wami.ui.theme.WamiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var serverConfig: ServerConfigStorage

    companion object {
        const val PREFS_NAME = "WamiPrefs"
        const val THEME_KEY = "theme_preference"
        const val CUSTOM_IP_KEY = "custom_ip"
        const val ENABLE_CUSTOM_IP_KEY = "enable_custom_ip"
        const val HIDDEN_CONVERSATIONS_KEY = "hidden_conversations"
        const val SERVER_CONFIG_PREFS_NAME = "server_config_pref"
    }

    private val authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) showAndCopySessionId() else toast("Authentication failed.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serverConfig = ServerConfigStorage(this)

        setContent {

            val theme by remember { mutableStateOf(prefs.getString(THEME_KEY, "system")!!) }
            var isCustomIpEnabled by remember { mutableStateOf(prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)) }

            var showThemeDialog by remember { mutableStateOf(false) }
            var showIpDialog by remember { mutableStateOf(false) }

            WamiTheme {
                SettingsScreen(
                    sessionId = serverConfig.getSessionId(),
                    theme = when (theme) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System Default"
                    },
                    isCustomIpEnabled = isCustomIpEnabled,
                    onCustomIpEnabledChange = {
                        isCustomIpEnabled = it
                        prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, it).apply()
                    },
                    onNavigateBack = { finish() },
                    onSessionClick = { triggerAuth() },
                    onLogoutClick = { confirm("Logout?", ::logout) },
                    onThemeClick = { showThemeDialog = true },
                    onSetCustomIpClick = { showIpDialog = true },
                    onResetHiddenConversationsClick = { confirm("Reset hidden chats?", ::resetHiddenConversations) },
                    onKillAppClick = { confirm("Force close app?", ::killApp) },
                    onResetPrefsClick = { confirm("Reset all settings?", ::resetAppPreferences) }
                )

                if (showThemeDialog) {
                    ThemeDialog(
                        currentTheme = theme,
                        onDismiss = { showThemeDialog = false },
                        onThemeSelected = { newTheme ->
                            prefs.edit().putString(THEME_KEY, newTheme).apply()
                            AppCompatDelegate.setDefaultNightMode(
                                when (newTheme) {
                                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                }
                            )
                            showThemeDialog = false
                        }
                    )
                }

                if (showIpDialog) {
                    CustomIpDialog(
                        currentIp = prefs.getString(CUSTOM_IP_KEY, "") ?: "",
                        onDismiss = { showIpDialog = false },
                        onConfirm = { newIp ->
                            prefs.edit().putString(CUSTOM_IP_KEY, newIp.trim()).apply()
                            showIpDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun triggerAuth() {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val intent = km.createConfirmDeviceCredentialIntent("Authentication Required", "Unlock to continue")
        if (km.isKeyguardSecure && intent != null) {
            authLauncher.launch(intent)
        } else {
            showAndCopySessionId()
        }
    }

    private fun showAndCopySessionId() {  }
    private fun resetHiddenConversations() {  }
    private fun resetAppPreferences() {  }
    private fun killApp() {  }
    private fun logout() {  }
    private fun confirm(msg: String, action: () -> Unit) {  }
    private fun toast(msg: String) {  }
}
