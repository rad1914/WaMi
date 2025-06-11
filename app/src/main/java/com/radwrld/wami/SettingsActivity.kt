// @path: app/src/main/java/com/radwrld/wami/Settings.kt

package com.radwrld.wami

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivitySettingsBinding
import com.radwrld.wami.storage.ServerConfigStorage

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "settings_prefs"
        const val ENABLE_CUSTOM_IP_KEY = "enable_custom_ip"
        const val CUSTOM_IP_KEY = "custom_ip"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serverConfigStorage = ServerConfigStorage(this)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
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
    }

    private fun loadSettings() {
        val useCustomIP = prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)
        binding.enableCustomIpSwitch.isChecked = useCustomIP
        binding.setCustomIpText.visibility = if (useCustomIP) View.VISIBLE else View.GONE
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
            // Save the new IP to our settings preferences.
            // ServerConfigStorage is already configured to read from this preference
            // when the 'Enable custom IP' switch is turned on.
            // This makes the entered IP the active server without overwriting the
            // original default server configuration.
            prefs.edit().putString(CUSTOM_IP_KEY, newIP).apply()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
