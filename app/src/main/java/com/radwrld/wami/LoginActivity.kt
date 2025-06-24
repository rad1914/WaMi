// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.radwrld.wami.databinding.ActivityLoginBinding
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.sync.SyncService
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLoginBinding
    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ServerConfigStorage(this)

        if (config.isLoggedIn() && !config.getSessionId().isNullOrEmpty()) {

            openMain()
            return
        }

        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        bind.offlineLoginButton.setOnClickListener { openMain() }
        bind.multiUserLoginButton.setOnClickListener { showSessionInput() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.uiState.collect(::render) }
                launch { vm.toastEvents.collect(::toast) }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isOnline()) vm.start() else vm.notifyNoNet()
    }

    private fun render(state: LoginUiState) = with(bind) {
        progressBar.isVisible = state is LoginUiState.Loading
        qrImage.isVisible = state is LoginUiState.ShowQr
        
        val isOfflineError = state is LoginUiState.Error && state.msg.contains("Sin conexión")
        offlineLoginButton.isVisible = isOfflineError
        
        multiUserLoginButton.isVisible = state !is LoginUiState.LoggedIn

        statusText.text = when (state) {
            is LoginUiState.Idle -> "WaMi"
            is LoginUiState.Loading -> state.msg
            is LoginUiState.ShowQr -> {
                qrImage.setImageBitmap(state.bitmap)
                state.msg
            }
            is LoginUiState.Error -> state.msg
            is LoginUiState.LoggedIn -> {
                openMain()
                "¡Vamos!"
            }
        }
    }

    private fun showSessionInput() {
        val input = EditText(this).apply { hint = "Ingresa ID de sesión" }
        AlertDialog.Builder(this)
            .setTitle("Inicio multi-usuario")
            .setMessage("Ingresa un ID de sesión existente")
            .setView(input)
            .setPositiveButton("Conectar") { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let {
                    vm.setSessionAndRestart(it)
                } ?: toast("El ID de sesión no puede estar vacío.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(net) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun openMain() {

        val serviceIntent = Intent(this, SyncService::class.java).apply { action = SyncService.ACTION_START }
        startService(serviceIntent)
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
