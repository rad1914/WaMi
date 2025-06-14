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

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var serverConfig: ServerConfigStorage

    companion object {
        const val PREFS_NAME = "settings_prefs"
        const val DARK_MODE_KEY = "dark_mode"
        const val CUSTOM_IP_KEY = "custom_ip"
        const val ENABLE_CUSTOM_IP_KEY = "enable_custom_ip"
        const val OFFLINE_MODE_KEY = "offline_mode"
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportSessionToUri(it) }
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
        binding.darkModeSwitch.isChecked = prefs.getBoolean(DARK_MODE_KEY, true)
        binding.darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(DARK_MODE_KEY, checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.enableCustomIpSwitch.isChecked = prefs.getBoolean(ENABLE_CUSTOM_IP_KEY, false)
        binding.setCustomIpText.visibility = if (binding.enableCustomIpSwitch.isChecked) View.VISIBLE else View.GONE
        binding.enableCustomIpSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ENABLE_CUSTOM_IP_KEY, checked).apply()
            binding.setCustomIpText.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) serverConfig.resetToPrimary()
        }

        binding.setCustomIpText.setOnClickListener { showCustomIpDialog() }

        binding.offlineModeSwitch.isChecked = prefs.getBoolean(OFFLINE_MODE_KEY, false)
        binding.offlineModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(OFFLINE_MODE_KEY, checked).apply()
        }

        binding.logoutText.setOnClickListener { confirm("Logout?", ::logout) }
        binding.exportSessionText.setOnClickListener { exportLauncher.launch("wami-session.zip") }
    }

    private fun showCustomIpDialog() {
        val input = EditText(this).apply {
            setText(prefs.getString(CUSTOM_IP_KEY, ""))
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
                    toast("Session exported.")
                } else {
                    toast("Export failed.")
                }
            } catch (e: Exception) {
                toast("Export error: ${e.message}")
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.getInstance(this@SettingsActivity).logout()
            } catch (_: Exception) {}
            ApiClient.close()
            serverConfig.saveSessionId(null)
            serverConfig.saveLoginState(false)
            startActivity(Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun confirm(msg: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
