// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/LoginViewModel.kt
package com.radwrld.wami.ui.viewmodel // <-- PAQUETE CORREGIDO

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import androidx.lifecycle.*
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.AuthError
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
    data class ShowQr(val bitmap: Bitmap, val msg: String) : LoginUiState()
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
        observeSyncState()
    }

    private fun observeSyncState() = viewModelScope.launch {
        launch {
            SyncManager.isAuthenticated.collect {
                if (it) {
                    config.saveLoginState(true)
                    uiState.value = LoginUiState.LoggedIn(config.getSessionId().orEmpty())
                }
            }
        }
        launch {
            SyncManager.authError.collect {
                if (it == AuthError.INVALID_SESSION) {
                    toastEvents.emit("Sesión inválida. Creando una nueva.")
                    config.saveSessionId(null)
                    start()
                }
            }
        }
        launch {
            SyncManager.socketState.collect { connected ->
                if (SyncManager.isAuthenticated.value) return@collect
                val msg = if (connected) "Conectado. Esperando código QR..." else "Desconectado. Reconectando..."
                if (SyncManager.qrCodeUrl.value == null || !connected) {
                    uiState.value = LoginUiState.Loading(msg)
                }
            }
        }
        launch {
            SyncManager.qrCodeUrl.collect { url ->
                url?.let { decodeQr(it)?.let { bmp -> uiState.value = LoginUiState.ShowQr(bmp, "Escanea el código QR") } }
            }
        }
    }

    fun start() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            if (!isOnline()) {
                uiState.value = LoginUiState.Error("Sin conexión: Por favor, conéctate a internet.")
                return@launch
            }
            try {
                uiState.value = LoginUiState.Loading("Estableciendo conexión...")
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

    private suspend fun handleError(e: Exception) {
        val msg = when (e) {
            is HttpException -> "Error del servidor: ${e.code()}"
            is UnknownHostException, is ConnectException, is SocketTimeoutException -> "Sin conexión: El servidor no está disponible."
            else -> "Error desconocido: ${e.message}"
        }
        uiState.value = LoginUiState.Error(msg)
        toastEvents.emit(msg)
    }

    private fun decodeQr(dataUrl: String): Bitmap? = try {
        val base64 = dataUrl.substringAfter(",", "")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        viewModelScope.launch { toastEvents.emit("Fallo al decodificar QR.") }
        null
    }

    private fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(net)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

class LoginViewModelFactory(
    private val app: Application,
    private val config: ServerConfigStorage
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(app, config) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
