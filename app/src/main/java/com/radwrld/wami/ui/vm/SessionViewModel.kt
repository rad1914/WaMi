// @path: app/src/main/java/com/radwrld/wami/ui/vm/SessionViewModel.kt
package com.radwrld.wami.ui.vm

import android.app.Application
import androidx.lifecycle.*
import com.radwrld.wami.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository(application)

    private val _sessionId = MutableStateFlow<String?>(null)
    private val _qrCode    = MutableStateFlow<String?>(null)
    private val _auth      = MutableStateFlow(false)

    val sessionId get() = _sessionId.asStateFlow()
    val qrCode    get() = _qrCode.asStateFlow()
    val isAuth    get() = _auth.asStateFlow()

    fun loginWithId(id: String) = viewModelScope.launch {
        val (ok, _) = withContext(Dispatchers.IO) { ApiService.getStatus(id) }
        if (ok) {
            prefs.saveSessionId(id)
            _sessionId.value = id
            _auth.value = true
        }
    }

    fun start() = viewModelScope.launch {
        prefs.sessionIdFlow.firstOrNull()?.let { id ->
            val (ok, _) = withContext(Dispatchers.IO) { ApiService.getStatus(id) }
            if (ok) {
                _sessionId.value = id
                _auth.value = true
                return@launch
            }
        }
        createNewSession()
    }

    private fun createNewSession() = viewModelScope.launch {
        val id = withContext(Dispatchers.IO) { ApiService.createSession() } ?: return@launch
        prefs.saveSessionId(id)
        _sessionId.value = id

        while (!_auth.value) {
            delay(3000)
            val (ok, qr) = withContext(Dispatchers.IO) { ApiService.getStatus(id) }
            _auth.value = ok
            _qrCode.value = qr
        }
    }

    fun logout() = viewModelScope.launch {
        sessionId.value?.let { id ->
            val success = withContext(Dispatchers.IO) { ApiService.logout(id) }
            if (success) {
                prefs.clearSessionId()
                _sessionId.value = null
                _auth.value = false
                _qrCode.value = null
            }
        }
    }
}

class ChatViewModel : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats get() = _chats.asStateFlow()

    fun load(sessionId: String) = viewModelScope.launch {
        _chats.value = withContext(Dispatchers.IO) { ApiService.fetchChats(sessionId) }
    }
}

class MessageViewModel : ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs get() = _msgs.asStateFlow()

    fun load(sessionId: String, jid: String) = viewModelScope.launch {
        _msgs.value = withContext(Dispatchers.IO) { ApiService.fetchHistory(sessionId, jid) }
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { ApiService.sendText(sessionId, jid, text) }
        load(sessionId, jid)
    }
}
