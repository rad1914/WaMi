// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/LoginViewModel.kt
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
    private val resolver: ContentResolver
) : AndroidViewModel(app) {

    val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val toastEvents = MutableSharedFlow<String>()

    private var job: Job? = null
    private var api = ApiClient.getInstance(app)

    private fun restartApi() {
        ApiClient.close()
        api = ApiClient.getInstance(getApplication())
    }

    fun start() {
        job?.cancel()
        restartApi()
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
                        uiState.value = LoginUiState.LoggedIn(config.getSessionId().orEmpty())
                        break
                    }
                    uiState.value = status.qr?.let { decodeQr(it)?.let { bmp -> LoginUiState.ShowQr(bmp, "Scan QR") } }
                        ?: LoginUiState.Loading("Waiting for QR...")
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        toastEvents.emit("Session expired, retrying...")
                        config.saveSessionId(null)
                        start()
                    } else handleError(e)
                    break
                } catch (e: Exception) {
                    handleError(e)
                    break
                }
                delay(5000)
            }
        }
    }

    fun setSessionAndRestart(sessionId: String) {
        job?.cancel()
        viewModelScope.launch {
            config.saveSessionId(sessionId)
            toastEvents.emit("Using session: $sessionId")
            start()
        }
    }

    fun notifyNoNet() {
        uiState.value = LoginUiState.Error("Please connect to the internet.")
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

    private suspend fun handleError(e: Exception) {
        val msg = when (e) {
            is HttpException -> "Server error ${e.code()}"
            is UnknownHostException -> "Offline: Server unreachable."
            is ConnectException -> "Offline: Connection failed."
            is SocketTimeoutException -> "Offline: Connection timed out."
            else -> "Unknown error: ${e.message}"
        }
        uiState.value = LoginUiState.Error(msg)
        toastEvents.emit(msg)
    }

    private fun decodeQr(data: String): Bitmap? = try {
        val base64 = data.substringAfter(",")
        BitmapFactory.decodeByteArray(Base64.decode(base64, Base64.DEFAULT), 0, base64.length)
    } catch (_: Exception) {
        viewModelScope.launch { toastEvents.emit("QR decode failed.") }
        null
    }
}

class LoginViewModelFactory(
    private val app: Application,
    private val config: ServerConfigStorage,
    private val resolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(cls: Class<T>): T {
        return LoginViewModel(app, config, resolver) as T
    }
}
