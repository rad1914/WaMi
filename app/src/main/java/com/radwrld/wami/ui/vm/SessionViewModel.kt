// @path: app/src/main/java/com/radwrld/wami/ui/vm/SessionViewModel.kt
package com.radwrld.wami.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import com.radwrld.wami.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionViewModel(application: Application): AndroidViewModel(application) {
    private val userPrefsRepository = UserPreferencesRepository(application)

    private val _sessionId = MutableStateFlow<String?>(null)
    private val _qrCode    = MutableStateFlow<String?>(null)
    private val _auth      = MutableStateFlow(false)
    val sessionId = _sessionId.asStateFlow()
    val qrCode    = _qrCode.asStateFlow()
    val isAuth    = _auth.asStateFlow()

    fun start() {
        viewModelScope.launch {

            val savedSessionId = userPrefsRepository.sessionIdFlow.first()

            if (savedSessionId != null) {

                val (ok, _) = withContext(Dispatchers.IO) { ApiService.getStatus(savedSessionId) }
                if (ok) {

                    _sessionId.value = savedSessionId
                    _auth.value = true
                    return@launch
                }
            }

            createNewSession()
        }
    }

    private fun createNewSession() = viewModelScope.launch {
        val id = withContext(Dispatchers.IO) {
            ApiService.createSession()
        } ?: return@launch

        userPrefsRepository.saveSessionId(id)
        _sessionId.value = id

        while (!_auth.value) {
            delay(3000)
            val (ok, qr) = withContext(Dispatchers.IO) {
                ApiService.getStatus(id)
            }
            
            _auth.value = ok
            _qrCode.value = qr
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionId.value?.let { currentSessionId ->
                val success = withContext(Dispatchers.IO) {
                    ApiService.logout(currentSessionId)
                }
                if (success) {

                    userPrefsRepository.clearSessionId()
                    _sessionId.value = null
                    _auth.value = false
                    _qrCode.value = null
                }
            }
        }
    }
}
