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
import com.radwrld.wami.network.SyncService
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
            val themeState by remember { mutableStateOf(prefs.getString(THEME_KEY, "system")!!) }
            var isCustomIpEnabled by remember { mutableStateOf(prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)) }
            var showThemeDialog by remember { mutableStateOf(false) }
            var showIpDialog by remember { mutableStateOf(false) }

            WamiTheme {
                SettingsScreen(
                    sessionId = serverConfig.getSessionId(),
                    theme = when (themeState) {
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
                    onLogoutClick = { confirm("Logout?", "This will delete the session and all local data.", ::logout) },
                    onThemeClick = { showThemeDialog = true },
                    onSetCustomIpClick = { showIpDialog = true },
                    onResetHiddenConversationsClick = { confirm("Reset hidden chats?", "All currently hidden chats will become visible again.", ::resetHiddenConversations) },
                    onKillAppClick = { confirm("Force close app?", "The application will be terminated immediately.", ::killApp) },
                    onResetPrefsClick = { confirm("Reset all settings?", "This will clear all app preferences and log you out.", ::resetAppPreferences) }
                )

                if (showThemeDialog) {
                    ThemeDialog(
                        currentTheme = themeState,
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
                            val trimmedIp = newIp.trim()
                            prefs.edit().putString(CUSTOM_IP_KEY, trimmedIp).apply()

                            ApiClient.close()
                            toast("Custom IP updated. Restart the app if you experience issues.")
                            showIpDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun triggerAuth() {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val intent = km.createConfirmDeviceCredentialIntent("Authentication Required", "Unlock to view and copy Session ID")
        if (km.isKeyguardSecure && intent != null) {
            authLauncher.launch(intent)
        } else {

            showAndCopySessionId()
        }
    }

    private fun showAndCopySessionId() {
        val sessionId = serverConfig.getSessionId()
        if (sessionId.isNullOrEmpty()) {
            toast("Session ID not found.")
            return
        }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Session ID", sessionId)
        clipboard.setPrimaryClip(clip)
        toast("Session ID copied to clipboard.")
    }

    private fun resetHiddenConversations() {
        prefs.edit().remove(HIDDEN_CONVERSATIONS_KEY).apply()
        toast("Hidden conversations have been reset.")
    }

    private fun resetAppPreferences() {

        getSharedPreferences(SERVER_CONFIG_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        prefs.edit().clear().apply()

        ApiClient.close()
        stopService(Intent(this, SyncService::class.java))

        toast("All preferences have been reset. Restarting...")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {

                Thread.sleep(1000)
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                val componentName = intent!!.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                startActivity(mainIntent)
                exitProcess(0)
            }
        }
    }

    private fun killApp() {
        exitProcess(0)
    }

    private fun logout() {
        lifecycleScope.launch {

            val stopIntent = Intent(this@SettingsActivity, SyncService::class.java)
            stopIntent.action = SyncService.ACTION_STOP
            startService(stopIntent)

            withContext(Dispatchers.IO) {
                runCatching { ApiClient.getInstance(this@SettingsActivity).logout() }
            }

            getSharedPreferences(SERVER_CONFIG_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
            prefs.edit().clear().apply()
            ApiClient.close()

            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
