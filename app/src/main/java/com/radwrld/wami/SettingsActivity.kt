// @path: app/src/main/java/com/radwrld/wami/SettingsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.content.SharedPreferences
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportSessionToUri(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importSessionFromUri(it) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serverConfig = ServerConfigStorage(this)

        initUI()
    }

    private fun initUI() {
        setupThemeSelector()

        // --- Network Settings ---
        binding.enableCustomIpSwitch.isChecked = prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)
        binding.setCustomIpText.visibility = if (binding.enableCustomIpSwitch.isChecked) View.VISIBLE else View.GONE
        binding.enableCustomIpSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, checked).apply()
            binding.setCustomIpText.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.setCustomIpText.setOnClickListener { showCustomIpDialog() }

        // --- Account Settings ---
        binding.logoutText.setOnClickListener { confirm("Logout?", "This will delete your local session.", ::logout) }
        binding.exportSessionText.setOnClickListener { exportLauncher.launch("wami-session.zip") }
        binding.importSessionText.setOnClickListener {
            confirm(
                "Import Session Data?",
                "This will overwrite your current session on the server. Please log in again after importing.",
                { importLauncher.launch("application/zip") }
            )
        }


        // --- Experimental Settings ---
        binding.offlineModeSwitch.isChecked = prefs.getBoolean(OFFLINE_MODE_KEY, false)
        binding.offlineModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(OFFLINE_MODE_KEY, checked).apply()
        }

        // The 'pruneAppDataText' listener has been removed.
        
        binding.removeAppCacheText.setOnClickListener { confirm("Remove App Cache?", "This can free up storage space.", ::removeAppCache) }
        binding.resetHiddenConversationsText.setOnClickListener { confirm("Reset Hidden Conversations?", "Your hidden conversations will become visible.", ::resetHiddenConversations) }
        binding.resetAppPrefsText.setOnClickListener { confirm("Reset All App Preferences?", "This will log you out and reset all settings. The app will close.", ::resetAppPreferences) }
        binding.killAppText.setOnClickListener { confirm("Kill The App?", "This is for debugging purposes and will force the app to close.", ::killApp) }

    }

    private fun setupThemeSelector() {
        updateThemeValueText()
        binding.themeSettingRow.setOnClickListener {
            val themes = arrayOf("Light", "Dark", "System Default")
            val themeValues = arrayOf("light", "dark", "system")
            val currentThemeValue = prefs.getString(THEME_KEY, "system")
            val checkedItem = themeValues.indexOf(currentThemeValue).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                    val selectedTheme = themeValues[which]
                    prefs.edit().putString(THEME_KEY, selectedTheme).apply()
                    updateThemeValueText()

                    val nightMode = when (selectedTheme) {
                        "light" -> AppCompatDelegate.MODE_NIGHT_NO
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    AppCompatDelegate.setDefaultNightMode(nightMode)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateThemeValueText() {
        binding.themeValueText.text = when (prefs.getString(THEME_KEY, "system")) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "System Default"
        }
    }

    private fun showCustomIpDialog() {
        val input = EditText(this).apply {
            setText(prefs.getString(CUSTOM_IP_KEY, ""))
            hint = "e.g., 192.168.1.100:8080"
        }
        AlertDialog.Builder(this)
            .setTitle("Set Custom Backend IP")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                prefs.edit().putString(CUSTOM_IP_KEY, input.text.toString().trim()).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportSessionToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getDownloadingInstance(this@SettingsActivity).exportSession()
                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            response.body()!!.byteStream().copyTo(out)
                        }
                    }
                    toast("Session exported successfully.")
                } else {
                    toast("Export failed: ${response.message()}")
                }
            } catch (e: Exception) {
                toast("Export error: ${e.message}")
            }
        }
    }
    
    private fun importSessionFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    toast("Failed to open file.")
                    return@launch
                }
                
                val fileBytes = inputStream.readBytes()
                inputStream.close()
                val requestFile = fileBytes.toRequestBody("application/zip".toMediaTypeOrNull())

                val body = MultipartBody.Part.createFormData("file", "wami-session.zip", requestFile)

                val response = ApiClient.getInstance(this@SettingsActivity).importSession(body)
                if (response.isSuccessful) {
                    toast("Session imported. Please log in again.")
                    logout()
                } else {
                    toast("Import failed: ${response.errorBody()?.string() ?: response.message()}")
                }
            } catch (e: Exception) {
                toast("Import error: ${e.message}")
            }
        }
    }

    private fun removeAppCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = deleteDir(cacheDir)
            withContext(Dispatchers.Main) {
                if (success) {
                    toast("App cache cleared.")
                } else {
                    toast("Failed to clear app cache.")
                }
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null) return true
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    if (!deleteDir(File(dir, child))) {
                        return false
                    }
                }
            }
        }
        return dir.delete()
    }
    
    private fun resetHiddenConversations() {
        prefs.edit().remove(HIDDEN_CONVERSATIONS_KEY).apply()
        toast("Hidden conversations list has been reset.")
    }

    private fun resetAppPreferences() {
        // Clear both preference files used by the app
        prefs.edit().clear().apply()
        getSharedPreferences(SERVER_CONFIG_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()

        toast("All app preferences have been reset. The app will now close.")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { Thread.sleep(1500) } // Give toast time to show
            // A restart is required for changes to take full effect.
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            exitProcess(0)
        }
    }
    
    private fun killApp() {
        finishAffinity()
        exitProcess(0)
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.getInstance(this@SettingsActivity).logout()
            } catch (e: Exception) {
                // Ignore network errors on logout, as we are clearing local data anyway
            }
            ApiClient.close()
            serverConfig.saveSessionId(null)
            serverConfig.saveLoginState(false)
            val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun confirm(title: String, msg: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
