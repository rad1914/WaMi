// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt

package com.radwrld.wami

import android.app.Application
import android.content.ContentResolver
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull


sealed class LoginUiState {
    object Idle : LoginUiState()
    data class Loading(val message: String) : LoginUiState()
    data class ShowQr(val qrBitmap: Bitmap, val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    object LoggedIn : LoginUiState()
}

class LoginViewModel(
    application: Application,
    private val config: ServerConfigStorage,
    private val contentResolver: ContentResolver
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents = _toastEvents.asSharedFlow()

    private var pollingJob: Job? = null
    private var waApi: WhatsAppApi = ApiClient.getInstance(application)
    private var failoverAttempted = false

    companion object {
        private const val POLLING_INTERVAL_MS = 5000L
        private const val RETRY_DELAY_MS = 10000L
    }

    private fun reinitializeApi() {
        waApi = ApiClient.getInstance(getApplication())
    }

    fun startLoginProcess() {
        pollingJob?.cancel()
        failoverAttempted = false
        config.resetToPrimary()
        reinitializeApi()
        executeAuthFlow()
    }

    private fun executeAuthFlow() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            try {
                if (config.getSessionId().isNullOrEmpty()) {
                    _uiState.value = LoginUiState.Loading("Creating new session...")
                    val session = waApi.createSession()
                    config.saveSessionId(session.sessionId)
                    reinitializeApi()
                }
                pollStatus()
            } catch (e: Exception) {
                handleError(e, true)
            }
        }
    }

    private fun pollStatus() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val status = waApi.getStatus()
                    if (status.connected) {
                        config.saveLoginState(true)
                        _uiState.value = LoginUiState.LoggedIn
                        break
                    } else {
                        _uiState.value = status.qr?.let { 
                            decodeQr(it)?.let { bitmap -> 
                                LoginUiState.ShowQr(bitmap, "Scan QR code with WhatsApp") 
                            }
                        } ?: LoginUiState.Loading("Waiting for QR code...")
                    }
                } catch (e: Exception) {
                    handleError(e, false)
                    break
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    fun importSession(uri: Uri) {
        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading("Importing session...")
            try {
                if (config.getSessionId().isNullOrEmpty()) {
                    val session = waApi.createSession()
                    config.saveSessionId(session.sessionId)
                    reinitializeApi()
                }

                val body = contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toRequestBody("application/zip".toMediaTypeOrNull())
                        .let { rb -> MultipartBody.Part.createFormData("sessionFile", "session.zip", rb) }
                } ?: run {
                    _toastEvents.emit("Failed to read session file.")
                    startLoginProcess()
                    return@launch
                }

                waApi.importSession(body)
                _toastEvents.emit("Import successful. Connecting...")
                delay(3000)
                pollStatus()

            } catch (e: Exception) {
                _toastEvents.emit("Import failed. Please try again.")
                config.saveLoginState(false)
                config.saveSessionId(null)
                delay(1000)
                startLoginProcess()
            }
        }
    }

    private suspend fun handleError(e: Exception, isInitial: Boolean) {
        when (e) {
            is HttpException -> {
                if (e.code() == 401) {
                    _toastEvents.emit("Session invalid. Creating a new one.")
                    config.saveLoginState(false)
                    config.saveSessionId(null)
                    delay(1000)
                    startLoginProcess()
                } else {
                    _uiState.value = LoginUiState.Error("Server Error (${e.code()}). Retrying...")
                    delay(RETRY_DELAY_MS)
                    if (isInitial) executeAuthFlow() else pollStatus()
                }
            }
            is UnknownHostException -> {
                if (failoverAttempted) {
                    _uiState.value = LoginUiState.Error("Both servers unreachable. Check settings or network.")
                } else {
                    failoverAttempted = true
                    _toastEvents.emit("Connection failed. Trying fallback server...")
                    config.moveToNextServer()
                    reinitializeApi()
                    delay(1000)
                    executeAuthFlow()
                }
            }
            else -> {
                _uiState.value = LoginUiState.Error("An unknown error occurred. Retrying...")
                delay(RETRY_DELAY_MS)
                if (isInitial) executeAuthFlow() else pollStatus()
            }
        }
    }

    private fun decodeQr(data: String): Bitmap? = try {
        val base64 = data.substringAfter(",")
        BitmapFactory.decodeByteArray(Base64.decode(base64, Base64.DEFAULT), 0, base64.length)
    } catch (_: Exception) {
        viewModelScope.launch { _toastEvents.emit("Failed to decode QR Code.") }
        null
    }

    fun notifyNoNetwork() {
        _uiState.value = LoginUiState.Error("Please connect to the internet to log in.")
    }

    fun logout() {
        pollingJob?.cancel()
        viewModelScope.launch {
            config.saveLoginState(false)
            config.saveSessionId(null)
            config.resetToPrimary()
            reinitializeApi()
            _uiState.value = LoginUiState.Idle
            _toastEvents.emit("Logged out.")
        }
    }
}

class LoginViewModelFactory(
    private val application: Application,
    private val config: ServerConfigStorage,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LoginViewModel(application, config, contentResolver) as T
}

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let(viewModel::importSession)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.importSessionButton.setOnClickListener {
            pickFileLauncher.launch("application/zip")
        }

        observeViewModel()

        if (isNetworkAvailable()) viewModel.startLoginProcess()
        else viewModel.notifyNoNetwork()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::updateUi)
                }
                launch {
                    viewModel.toastEvents.collect(::showToast)
                }
            }
        }
    }

    private fun updateUi(state: LoginUiState) = with(binding) {
        progressBar.isVisible = state is LoginUiState.Loading
        qrImage.isVisible = state is LoginUiState.ShowQr
        importSessionButton.isEnabled = state !is LoginUiState.Loading

        statusText.text = when (state) {
            is LoginUiState.Idle -> "WaMi"
            is LoginUiState.Loading -> state.message
            is LoginUiState.ShowQr -> {
                qrImage.setImageBitmap(state.qrBitmap)
                state.message
            }
            is LoginUiState.Error -> state.message
            is LoginUiState.LoggedIn -> {
                launchMainActivity()
                "Logged in successfully!"
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
