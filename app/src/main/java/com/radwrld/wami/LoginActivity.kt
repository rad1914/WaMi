// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.radwrld.wami.databinding.ActivityLoginBinding
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.UnknownHostException

sealed class LoginUiState {
    object Idle : LoginUiState()
    object LoggedIn : LoginUiState()
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
                    uiState.value = LoginUiState.Loading("Creating session...")
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
                        uiState.value = LoginUiState.LoggedIn
                        break
                    }
                    uiState.value = status.qr?.let { decodeQr(it)?.let { bmp -> LoginUiState.ShowQr(bmp, "Scan QR") } }
                        ?: LoginUiState.Loading("Waiting for QR...")
                } catch (e: Exception) {
                    handleError(e)
                    break
                }
                delay(DELAY)
            }
        }
    }

    fun import(uri: Uri) {
        job?.cancel()
        viewModelScope.launch {
            uiState.value = LoginUiState.Loading("Importing session...")
            try {
                if (config.getSessionId().isNullOrEmpty()) {
                    config.saveSessionId(api.createSession().sessionId)
                    restartApi()
                }

                val body = resolver.openInputStream(uri)?.readBytes()?.toRequestBody()
                    ?.let { MultipartBody.Part.createFormData("sessionFile", "session.zip", it) }

                if (body == null) {
                    toastEvents.emit("Failed to read session file.")
                    start()
                    return@launch
                }

                api.importSession(body)
                toastEvents.emit("Import successful.")
                delay(3000)
                pollStatus()
            } catch (e: Exception) {
                toastEvents.emit("Import failed.")
                config.saveLoginState(false)
                config.saveSessionId(null)
                delay(1000)
                start()
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        when (e) {
            is HttpException -> {
                if (e.code() == 401) {
                    toastEvents.emit("Session expired. Creating new.")
                    config.saveLoginState(false)
                    config.saveSessionId(null)
                    delay(1000)
                    start()
                } else {
                    uiState.value = LoginUiState.Error("Server error ${e.code()}")
                }
            }
            is UnknownHostException -> uiState.value = LoginUiState.Error("Server unreachable.")
            else -> uiState.value = LoginUiState.Error("Unexpected error.")
        }
    }

    private fun decodeQr(data: String): Bitmap? = try {
        val base64 = data.substringAfter(",")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        viewModelScope.launch { toastEvents.emit("QR decode failed.") }
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
            toastEvents.emit("Logged out.")
        }
    }
}

class LoginViewModelFactory(
    private val app: Application,
    private val config: ServerConfigStorage,
    private val resolver: android.content.ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is assignable from LoginViewModel
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            // Suppress the "UNCHECKED_CAST" warning as we have verified the class type
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(app, config, resolver) as T
        }
        // If it's an unknown class, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LoginActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLoginBinding
    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let(vm::import)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        bind.importSessionButton.setOnClickListener { filePicker.launch("application/zip") }
        bind.offlineLoginButton.setOnClickListener { openMain() }

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
        importSessionButton.isEnabled = state !is LoginUiState.Loading
        offlineLoginButton.isVisible = state is LoginUiState.Error && state.msg == LoginViewModel.OFFLINE

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
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
