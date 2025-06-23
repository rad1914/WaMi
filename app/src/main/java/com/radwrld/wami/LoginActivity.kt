// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.app.Application
import android.content.*
import android.graphics.*
import android.net.*
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.radwrld.wami.databinding.ActivityLoginBinding
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.net.*

class LoginActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLoginBinding
    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (vm.uiState.value is LoginUiState.Idle || vm.uiState.value is LoginUiState.Error) {
            if (isOnline()) vm.start() else vm.notifyNoNet()
        }
    }

    private fun render(state: LoginUiState) = with(bind) {
        progressBar.isVisible = state is LoginUiState.Loading
        qrImage.isVisible = state is LoginUiState.ShowQr
        offlineLoginButton.isVisible = state is LoginUiState.Error &&
                (state.msg.startsWith("Offline:") || state.msg.contains("connect to the internet"))
        multiUserLoginButton.isVisible = state is LoginUiState.Idle || state is LoginUiState.Error || state is LoginUiState.ShowQr

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
                "Let's Go!"
            }
        }
    }

    private fun showSessionInput() {
        val input = EditText(this).apply { hint = "Enter Session ID" }
        AlertDialog.Builder(this)
            .setTitle("Multi-user Login")
            .setMessage("Enter existing session ID")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let {
                    vm.setSessionAndRestart(it)
                } ?: toast("Session ID cannot be empty.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isOnline(): Boolean {
        val net = (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork ?: return false
        return (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)
            .getNetworkCapabilities(net)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
