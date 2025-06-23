package com.radwrld.wami

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// --- IMPORTACIÓN CORREGIDA ---
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.sync.SyncManager
// --- FIN DE LA CORRECCIÓN ---
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
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
    private val config: ServerConfigStorage
) : AndroidViewModel(app) {

    val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val toastEvents = MutableSharedFlow<String>()

    private var sessionJob: Job? = null

    init {
        // Observar los estados de SyncManager en tiempo real
        viewModelScope.launch {
            SyncManager.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated) {
                    config.saveLoginState(true)
                    uiState.value = LoginUiState.LoggedIn(config.getSessionId().orEmpty())
                }
            }
        }

        viewModelScope.launch {
            SyncManager.qrCodeUrl.collect { qrUrl ->
                if (qrUrl != null) {
                    decodeQr(qrUrl)?.let { bitmap ->
                        uiState.value = LoginUiState.ShowQr(bitmap, "Escanea el código QR")
                    }
                } else {
                    // Si el QR es nulo, pero no estamos autenticados, mostramos "cargando"
                    if (!SyncManager.isAuthenticated.value) {
                       uiState.value = LoginUiState.Loading("Esperando código QR...")
                    }
                }
            }
        }
    }

    private fun getApi() = ApiClient.getInstance(getApplication())

    fun start() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            try {
                // Asegurarse de que SyncManager esté inicializado y conectado
                SyncManager.initialize(getApplication())
                SyncManager.connect()

                if (config.getSessionId().isNullOrEmpty()) {
                    uiState.value = LoginUiState.Loading("Creando nueva sesión...")
                    val newSession = getApi().createSession()
                    config.saveSessionId(newSession.sessionId)
                    // Reiniciar SyncManager con el nuevo token
                    SyncManager.shutdown()
                    SyncManager.initialize(getApplication())
                    SyncManager.connect()
                }
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
            SyncManager.shutdown() // Forzar reinicio del socket con el nuevo token
            start()
        }
    }

    fun notifyNoNet() {
        uiState.value = LoginUiState.Error("Por favor, conéctate a internet.")
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
        val base64String = dataUrl.substringAfter(",", "")
        if (base64String.isEmpty()) throw IllegalArgumentException("Invalid data URL")

        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
    override fun <T : ViewModel> create(cls: Class<T>): T {
        if (cls.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(app, config) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
