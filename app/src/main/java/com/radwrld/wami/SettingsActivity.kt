package com.radwrld.wami

import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.databinding.ActivitySettingsBinding
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.HiddenConversationStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var hiddenConversationStorage: HiddenConversationStorage
    private lateinit var prefs: SharedPreferences
    private lateinit var api: WhatsAppApi

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        uri?.let { saveSessionToFile(it) }
    }

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
        api = ApiClient.getInstance(this)

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        binding.enableCustomIpSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.setCustomIpText.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, isChecked).apply()
            if (!isChecked) {
                serverConfigStorage.resetToPrimary()
            }
        }

        binding.setCustomIpText.setOnClickListener {
            showSetCustomIPDialog()
        }

        binding.logoutText.setOnClickListener {
            showConfirmationDialog(
                "Logout?",
                "This will log you out and delete your session from this device. You will need to scan a new QR code to log in again."
            ) { logout() }
        }

        binding.exportSessionText.setOnClickListener {
            exportSession()
        }

        binding.importSessionText.setOnClickListener {
            Toast.makeText(this, "Import session feature is not yet implemented.", Toast.LENGTH_LONG).show()
        }

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

    private fun logout() {
        lifecycleScope.launch {
            try {
                api.logout()
                Toast.makeText(this@SettingsActivity, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Server logout failed, proceeding with client-side cleanup", e)
            } finally {
                ApiClient.close()
                serverConfigStorage.saveSessionId(null)
                serverConfigStorage.saveLoginState(false)

                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun exportSession() {
        val sessionId = serverConfigStorage.getSessionId()?.substring(0, 8) ?: "session"
        val fileName = "wami-session-$sessionId.zip"
        createFileLauncher.launch(fileName)
    }

    private fun saveSessionToFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@SettingsActivity, "Exporting session...", Toast.LENGTH_SHORT).show()
                val response = api.exportSession()

                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            response.body()!!.byteStream().copyTo(outputStream)
                        }
                    }
                    Toast.makeText(this@SettingsActivity, "Session exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@SettingsActivity, "Export failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Failed to export session", e)
                Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
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

    private fun resetHiddenConversations() {
        hiddenConversationStorage.clearAll()
        Toast.makeText(this, "All hidden conversations have been reset.", Toast.LENGTH_SHORT).show()
    }

    private fun pruneAppData() {
        try {
            (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.clearApplicationUserData()
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
        recreate()
    }
}
