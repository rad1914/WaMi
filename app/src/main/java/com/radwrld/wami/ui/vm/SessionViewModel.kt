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
    private val _qrCode = MutableStateFlow<String?>(null)
    private val _auth = MutableStateFlow(false)

    val sessionId = _sessionId.asStateFlow()
    val qrCode = _qrCode.asStateFlow()
    val isAuth = _auth.asStateFlow()

    fun loginWithId(id: String) = launch {
        val (ok, _) = ApiService.getStatus(id)
        if (ok) {
            prefs.saveSessionId(id)
            _sessionId.value = id
            _auth.value = true
        }
    }

    fun start() = launch {
        val id = prefs.sessionIdFlow.firstOrNull()
        if (id != null && ApiService.getStatus(id).first) {
            _sessionId.value = id
            _auth.value = true
        } else {
            createNewSession()
        }
    }

    private fun createNewSession() = launch {
        val id = ApiService.createSession() ?: return@launch
        prefs.saveSessionId(id)
        _sessionId.value = id

        while (isActive && !_auth.value) {
            delay(3000)
            val (ok, qr) = ApiService.getStatus(id)
            _auth.value = ok
            _qrCode.value = qr
        }
    }

    fun logout() = launch {
        sessionId.value?.let { id ->
            if (ApiService.logout(id)) {
                prefs.clearSessionId()
                _sessionId.value = null
                _auth.value = false
                _qrCode.value = null
            }
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } catch (e: Exception) { e.printStackTrace() }
        }
}

class ChatViewModel : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    fun load(sessionId: String) = launch {
        _chats.value = ApiService.fetchChats(sessionId)
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } catch (e: Exception) {
                e.printStackTrace()
                _chats.value = emptyList()
            }
        }
}

class MessageViewModel : ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(sessionId: String, jid: String) = launch {
        _msgs.value = ApiService.fetchHistory(sessionId, jid)
    }

    fun send(sessionId: String, jid: String, text: String) = launch {
        ApiService.sendText(sessionId, jid, text)
        load(sessionId, jid)
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } catch (e: Exception) {
                e.printStackTrace()
                _msgs.value = emptyList()
            }
        }
}
