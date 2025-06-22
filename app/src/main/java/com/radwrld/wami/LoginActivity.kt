// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import java.net.ConnectException
import java.net.UnknownHostException

sealed class LoginUiState {
    object Idle : LoginUiState()
    data class LoggedIn(val sessionId: String) : LoginUiState()
    data class Loading(val msg: String) : LoginUiState()
    data class ShowQr(val bitmap: Bitmap, val msg: String) : LoginUiState()
    data class Error(val msg: String) : LoginUiState()
}

class LoginViewModel(
    app: Application,
    private val config: ServerConfigStorage,
    private val resolver: android.content.ContentResolver
) : AndroidViewModel(app) {

    val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val toastEvents = MutableSharedFlow<String>()

    private var job: Job? = null
    private var api: WhatsAppApi = ApiClient.getInstance(app)

    companion object {
        private const val DELAY = 5000L
        const val OFFLINE = "Please connect to the internet to log in."
    }

    fun start() {
        job?.cancel()
        restartApi()
        loginFlow()
    }

    private fun restartApi() {
        ApiClient.close()
        api = ApiClient.getInstance(getApplication())
    }

    private fun loginFlow() {
        job = viewModelScope.launch {
            try {
                if (config.getSessionId().isNullOrEmpty()) {
                    uiState.value = LoginUiState.Loading("Creating new session...")
                    config.saveSessionId(api.createSession().sessionId)
                    restartApi()
                }
                pollStatus()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun pollStatus() {
        job = viewModelScope.launch {
            while (isActive) {
                try {
                    val status = api.getStatus()
                    if (status.connected) {
                        config.saveLoginState(true)
                        val sessionId = config.getSessionId() ?: "Unknown"
                        uiState.value = LoginUiState.LoggedIn(sessionId)
                        break
                    }
                    uiState.value = status.qr?.let { decodeQr(it)?.let { bmp -> LoginUiState.ShowQr(bmp, "Scan QR to log in") } }
                        ?: LoginUiState.Loading("Waiting for QR Code...")
                } catch (e: Exception) {
                    if (e is HttpException && e.code() == 401) {
                        toastEvents.emit("Session expired. Creating a new one.")
                        config.saveSessionId(null)
                        loginFlow()
                        break
                    }
                    handleError(e)
                    break
                }
                delay(DELAY)
            }
        }
    }
    
    fun setSessionAndRestart(sessionId: String) {
        job?.cancel()
        viewModelScope.launch {
            config.saveSessionId(sessionId)
            toastEvents.emit("Attempting to connect with session: $sessionId")
            start()
        }
    }

    private suspend fun handleError(e: Exception) {
        Log.e("LoginViewModel", "Login failed", e)
        val baseUrl = ApiClient.getBaseUrl(getApplication())
        val verboseError: String
        when (e) {
            is HttpException -> {
                val errorMsg = "Server error: ${e.code()}"
                uiState.value = LoginUiState.Error(errorMsg)
                verboseError = "$errorMsg\nURL: ${e.response()?.raw()?.request?.url}"
            }
            is UnknownHostException -> {
                val errorMsg = "Server unreachable."
                uiState.value = LoginUiState.Error(errorMsg)
                verboseError = "$errorMsg $baseUrl"
            }
            is ConnectException -> {
                 val errorMsg = "Connection failed."
                 uiState.value = LoginUiState.Error(errorMsg)
                 verboseError = "$errorMsg\nIs the server running at $baseUrl?\nDetails: ${e.message}"
            }
            else -> {
                val errorMsg = "Unknown Error"
                uiState.value = LoginUiState.Error(errorMsg)
                verboseError = "$errorMsg Details: ${e.message}"
            }
        }
        toastEvents.emit(verboseError)
    }

    private fun decodeQr(data: String): Bitmap? = try {
        val base64 = data.substringAfter(",")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        viewModelScope.launch { toastEvents.emit("Failed to decode QR image.") }
        null
    }

    fun notifyNoNet() {
        uiState.value = LoginUiState.Error(OFFLINE)
    }

    fun logout() {
        job?.cancel()
        viewModelScope.launch {
            config.saveLoginState(false)
            config.saveSessionId(null)
            restartApi()
            uiState.value = LoginUiState.Idle
            toastEvents.emit("Logged out successfully.")
        }
    }
}

class LoginViewModelFactory(
    private val app: Application,
    private val config: ServerConfigStorage,
    private val resolver: android.content.ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(app, config, resolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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
        
        bind.multiUserLoginButton.setOnClickListener { showMultiUserInputDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.uiState.collect(::render) }
                launch { vm.toastEvents.collect(::toast) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val state = vm.uiState.value
        if (state is LoginUiState.Idle || state is LoginUiState.Error) {
            if (isOnline()) vm.start() else vm.notifyNoNet()
        }
    }

    private fun render(state: LoginUiState) = with(bind) {
        progressBar.isVisible = state is LoginUiState.Loading
        qrImage.isVisible = state is LoginUiState.ShowQr
        offlineLoginButton.isVisible = state is LoginUiState.Error && state.msg == LoginViewModel.OFFLINE
        
        // ++ CAMBIO: El botón ahora es visible también cuando se muestra el QR ++
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
                "Logged in as: ${state.sessionId}"
            }
        }
    }
    
    private fun showMultiUserInputDialog() {
        val input = EditText(this).apply {
            hint = "Enter Session ID (UUID)"
        }
        AlertDialog.Builder(this)
            .setTitle("Multi-user Login")
            .setMessage("Enter an existing session ID to connect without scanning a QR code.")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val sessionId = input.text.toString().trim()
                if (sessionId.isNotEmpty()) {
                    vm.setSessionAndRestart(sessionId)
                } else {
                    toast("Session ID cannot be empty.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
