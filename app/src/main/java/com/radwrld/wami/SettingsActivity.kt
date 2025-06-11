// @path: app/src/main/java/com/radwrld/wami/SettingsActivity.kt
package com.radwrld.wami

import android.app.ActivityManager
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivitySettingsBinding
import com.radwrld.wami.storage.HiddenConversationStorage
import com.radwrld.wami.storage.ServerConfigStorage

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var hiddenConversationStorage: HiddenConversationStorage
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "settings_prefs"
        const val ENABLE_CUSTOM_IP_KEY = "enable_custom_ip"
        const val CUSTOM_IP_KEY = "custom_ip"
        const val OFFLINE_MODE_KEY = "offline_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serverConfigStorage = ServerConfigStorage(this)
        hiddenConversationStorage = HiddenConversationStorage(this)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        // Custom IP Listeners
        binding.enableCustomIpSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.setCustomIpText.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, isChecked).apply()
            if (!isChecked) {
                // When disabled, ensure we fall back to the default primary server
                serverConfigStorage.resetToPrimary()
            }
        }

        binding.setCustomIpText.setOnClickListener {
            showSetCustomIPDialog()
        }

        // Experimental Settings Listeners
        binding.offlineModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(OFFLINE_MODE_KEY, isChecked).apply()
        }

        binding.pruneAppDataText.setOnClickListener {
            showConfirmationDialog(
                "Prune App Data?",
                "This will permanently delete all application data, including settings and accounts. The app will restart."
            ) { pruneAppData() }
        }

        binding.removeAppCacheText.setOnClickListener {
            showConfirmationDialog(
                "Remove App Cache?",
                "This will clear all cached data for the application."
            ) { removeAppCache() }
        }

        binding.resetHiddenConversationsText.setOnClickListener {
            showConfirmationDialog(
                "Reset Hidden Conversations?",
                "This will make all hidden conversations visible again."
            ) { resetHiddenConversations() }
        }

        binding.killAppText.setOnClickListener {
            showConfirmationDialog(
                "Kill App?",
                "This will forcefully terminate the application immediately."
            ) { killApp() }
        }

        binding.resetAppPrefsText.setOnClickListener {
            showConfirmationDialog(
                "Reset App Preferences?",
                "All application settings will be reset to their default values. The settings screen will reload."
            ) { resetAppPreferences() }
        }
    }

    private fun loadSettings() {
        val useCustomIP = prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)
        binding.enableCustomIpSwitch.isChecked = useCustomIP
        binding.setCustomIpText.visibility = if (useCustomIP) View.VISIBLE else View.GONE

        val offlineMode = prefs.getBoolean(OFFLINE_MODE_KEY, false)
        binding.offlineModeSwitch.isChecked = offlineMode
    }

    private fun showSetCustomIPDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Custom IP Address")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        val currentIP = prefs.getString(CUSTOM_IP_KEY, "")
        input.setText(currentIP)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val newIP = input.text.toString().trim()
            prefs.edit().putString(CUSTOM_IP_KEY, newIP).apply()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Action Functions ---

    private fun resetHiddenConversations() {
        hiddenConversationStorage.clearAll()
        Toast.makeText(this, "All hidden conversations have been reset.", Toast.LENGTH_SHORT).show()
    }

    private fun pruneAppData() {
        try {
            (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.clearApplicationUserData()
            // Note: This action will kill the app process and restart it with clean data.
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to prune app data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAppCache() {
        try {
            val success = cacheDir.deleteRecursively()
            val message = if (success) "App cache cleared." else "Failed to clear app cache."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to clear app cache.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun killApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun resetAppPreferences() {
        prefs.edit().clear().apply()
        Toast.makeText(this, "Preferences have been reset.", Toast.LENGTH_SHORT).show()
        // Reload the activity to reflect the default settings
        recreate()
    }
}
