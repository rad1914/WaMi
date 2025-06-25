// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/LoginViewModel.kt
package com.radwrld.wami

import android.app.Application
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.*
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SyncManager
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.net.*

sealed class LoginUiState {
    object Idle : LoginUiState()
    data class LoggedIn(val sessionId: String) : LoginUiState()
    data class Loading(val msg: String) : LoginUiState()
    data class ShowQr(val bitmap: android.graphics.Bitmap, val msg: String) : LoginUiState()
    data class Error(val msg: String) : LoginUiState()
}

class LoginViewModel(
    app: Application,
    private val config: ServerConfigStorage
) : AndroidViewModel(app) {

    val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val toastEvents = MutableSharedFlow<String>()
    private var sessionJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                SyncManager.isAuthenticated.collect {
                    if (it) {
                        config.saveLoginState(true)
                        uiState.value = LoginUiState.LoggedIn(config.getSessionId().orEmpty())
                    }
                }
            }
            launch {
                SyncManager.qrCodeUrl.collect { url ->
                    if (url != null) decodeQr(url)?.let {
                        uiState.value = LoginUiState.ShowQr(it, "Escanea el código QR")
                    } else if (!SyncManager.isAuthenticated.value) {
                        uiState.value = LoginUiState.Loading("Esperando código QR...")
                    }
                }
            }
            launch {
                SyncManager.authError.collect {
                    if (it) {
                        toastEvents.emit("Sesión inválida. Creando una nueva.")
                        config.saveSessionId(null)
                        start()
                    }
                }
            }
        }
    }

    fun start() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            try {
                if (config.getSessionId().isNullOrEmpty()) {
                    uiState.value = LoginUiState.Loading("Creando nueva sesión...")
                    val session = ApiClient.getInstance(getApplication()).createSession()
                    config.saveSessionId(session.sessionId)
                }
                SyncManager.shutdown()
                SyncManager.initialize(getApplication())
                SyncManager.connect()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun setSessionAndRestart(sessionId: String) {
        sessionJob?.cancel()
        viewModelScope.launch {
            config.saveSessionId(sessionId)
            toastEvents.emit("Usando sesión: $sessionId")
            start()
        }
    }

    fun notifyNoNet() {
        uiState.value = LoginUiState.Error("Por favor, conéctate a internet.")
    }

    private suspend fun handleError(e: Exception) {
        val msg = when (e) {
            is HttpException -> "Error del servidor: ${e.code()}"
            is UnknownHostException, is ConnectException, is SocketTimeoutException ->
                "Sin conexión: El servidor no está disponible."
            else -> "Error desconocido: ${e.message}"
        }
        uiState.value = LoginUiState.Error(msg)
        toastEvents.emit(msg)
    }

    private fun decodeQr(dataUrl: String) = try {
        val base64 = dataUrl.substringAfter(",", "")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        viewModelScope.launch { toastEvents.emit("Fallo al decodificar QR.") }
        null
    }
}

class LoginViewModelFactory(
    private val app: Application,
    private val config: ServerConfigStorage,
    private val resolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(app, config) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



