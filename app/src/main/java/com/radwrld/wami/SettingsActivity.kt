package com.radwrld.wami

import android.app.KeyguardManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.databinding.ActivitySettingsBinding
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var serverConfig: ServerConfigStorage

    companion object {
        const val PREFS_NAME = "WamiPrefs"
        const val THEME_KEY = "theme_preference"
        const val CUSTOM_IP_KEY = "custom_ip"
        const val ENABLE_CUSTOM_IP_KEY = "enable_custom_ip"
        const val OFFLINE_MODE_KEY = "offline_mode"
        const val HIDDEN_CONVERSATIONS_KEY = "hidden_conversations"
        const val SERVER_CONFIG_PREFS_NAME = "server_config_pref"
    }

    private val authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) showAndCopySessionId() else toast("Authentication failed.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serverConfig = ServerConfigStorage(this)

        setupTheme()
        setupSession()
        setupOptions()
    }

    private fun setupTheme() {
        updateThemeText()
        binding.themeSettingRow.setOnClickListener {
            val options = arrayOf("Light", "Dark", "System Default")
            val values = arrayOf("light", "dark", "system")
            val current = prefs.getString(THEME_KEY, "system")
            val selected = values.indexOf(current).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(options, selected) { dialog, which ->
                    prefs.edit().putString(THEME_KEY, values[which]).apply()
                    AppCompatDelegate.setDefaultNightMode(
                        when (values[which]) {
                            "light" -> AppCompatDelegate.MODE_NIGHT_NO
                            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                    updateThemeText()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateThemeText() {
        binding.themeValueText.text = when (prefs.getString(THEME_KEY, "system")) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "System Default"
        }
    }

    private fun setupSession() {
        val id = serverConfig.getSessionId()
        if (id.isNullOrBlank()) {
            binding.sessionIdLayout.visibility = View.GONE
            return
        }
        binding.sessionIdLayout.visibility = View.VISIBLE
        binding.sessionIdLayout.setOnClickListener {
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val intent = km.createConfirmDeviceCredentialIntent("Authentication Required", "Unlock to continue")
            if (km.isKeyguardSecure && intent != null) authLauncher.launch(intent)
            else showAndCopySessionId()
        }
    }

    private fun showAndCopySessionId() {
        val id = serverConfig.getSessionId() ?: return
        binding.sessionIdValueText.text = id
        AlertDialog.Builder(this)
            .setTitle("Session ID")
            .setMessage("Use this Token Identifier to log in on multiple devices without scanning a QR code.\n\n$id")
            .setPositiveButton("Copy") { d, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Session ID", id))
                toast("Copied to clipboard")
                d.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupOptions() {
        binding.enableCustomIpSwitch.isChecked = prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)
        binding.setCustomIpText.visibility = if (binding.enableCustomIpSwitch.isChecked) View.VISIBLE else View.GONE

        binding.enableCustomIpSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, checked).apply()
            binding.setCustomIpText.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.setCustomIpText.setOnClickListener { showCustomIpDialog() }
        binding.logoutText.setOnClickListener { confirm("Logout?", ::logout) }
        binding.offlineModeSwitch.isChecked = prefs.getBoolean(OFFLINE_MODE_KEY, false)
        binding.offlineModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(OFFLINE_MODE_KEY, checked).apply()
        }

        binding.removeAppCacheText.setOnClickListener { confirm("Clear cache?", ::removeAppCache) }
        binding.resetHiddenConversationsText.setOnClickListener { confirm("Reset hidden chats?", ::resetHiddenConversations) }
        binding.resetAppPrefsText.setOnClickListener { confirm("Reset all settings?", ::resetAppPreferences) }
        binding.killAppText.setOnClickListener { confirm("Force close app?", ::killApp) }
    }

    private fun showCustomIpDialog() {
        val input = EditText(this).apply {
            setText(prefs.getString(CUSTOM_IP_KEY, ""))
            hint = "127.0.0.1:3007"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Custom IP")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                prefs.edit().putString(CUSTOM_IP_KEY, input.text.toString().trim()).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeAppCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cleared = deleteDir(cacheDir)
            withContext(Dispatchers.Main) {
                toast(if (cleared) "Cache cleared." else "Failed to clear cache.")
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null) return true
        if (dir.isDirectory) dir.listFiles()?.forEach { if (!deleteDir(it)) return false }
        return dir.delete()
    }

    private fun resetHiddenConversations() {
        prefs.edit().remove(HIDDEN_CONVERSATIONS_KEY).apply()
        toast("Hidden chats reset.")
    }

    private fun resetAppPreferences() {
        prefs.edit().clear().apply()
        getSharedPreferences(SERVER_CONFIG_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        toast("App will now close.")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { Thread.sleep(1000) }
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(Intent.makeRestartActivityTask(intent!!.component))
            exitProcess(0)
        }
    }

    private fun killApp() {
        finishAffinity()
        exitProcess(0)
    }

    private fun logout() {
        lifecycleScope.launch {
            try { ApiClient.getInstance(this@SettingsActivity).logout() } catch (_: Exception) {}
            ApiClient.close()
            serverConfig.saveSessionId(null)
            serverConfig.saveLoginState(false)
            startActivity(Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun confirm(msg: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> action() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
