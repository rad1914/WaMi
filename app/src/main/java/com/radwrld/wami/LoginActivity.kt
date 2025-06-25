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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.radwrld.wami.databinding.ActivityLoginBinding
import com.radwrld.wami.network.SyncService
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLoginBinding
    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ServerConfigStorage(this).isLoggedIn()) {
            openMain(); return
        }

        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        bind.offlineLoginButton.setOnClickListener { openMain() }
        bind.multiUserLoginButton.setOnClickListener { showSessionInput() }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
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
        offlineLoginButton.isVisible = state is LoginUiState.Error && state.msg.contains("Sin conexión")
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
                openMain(); "¡Vamos!"
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
                val sessionId = input.text.toString()
                if (sessionId.isNotBlank()) vm.setSessionAndRestart(sessionId)
                else toast("El ID de sesión no puede estar vacío.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(net)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun openMain() {
        startService(Intent(this, SyncService::class.java).apply {
            action = SyncService.ACTION_START
        })
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
