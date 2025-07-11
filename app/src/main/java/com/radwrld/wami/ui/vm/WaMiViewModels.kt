// @path: app/src/main/java/com/radwrld/wami/ui/vm/WaMiViewModels.kt
package com.radwrld.wami.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repo: MessageRepository
) : ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(jid: String) = viewModelScope.launch {
        _msgs.value = repo.observeMessages(jid).firstOrNull().orEmpty()
        _msgs.value = repo.refreshMessages(jid)
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        if (repo.sendText(sessionId, jid, text)) {
            _msgs.update { listOf(Message(UUID.randomUUID().toString(), true, text, System.currentTimeMillis(), jid, "")) + it }
        }
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val api: ApiService
) : ViewModel() {
    private val _sessionId = MutableStateFlow<String?>(null)
    private val _qrCode = MutableStateFlow<String?>(null)
    private val _auth = MutableStateFlow(false)

    val sessionId = _sessionId.asStateFlow()
    val qrCode = _qrCode.asStateFlow()
    val isAuth = _auth.asStateFlow()

    fun start() = viewModelScope.launch {
        prefs.sessionIdFlow.firstOrNull()?.let { id ->
            if (api.getStatus(id)?.connected == true) {
                _sessionId.value = id
                _auth.value = true
                return@launch
            }
        }
        createSession()
    }

    private fun createSession() = viewModelScope.launch {
        api.createSession()?.let { id ->
            prefs.saveSessionId(id)
            _sessionId.value = id
            while (!_auth.value) {
                delay(3000)
                api.getStatus(id)?.let {
                    _auth.value = it.connected
                    _qrCode.value = it.qr
                }
            }
        }
    }

    fun loginWithId(id: String) = viewModelScope.launch {
        api.getStatus(id)?.takeIf { it.connected }?.let {
            prefs.saveSessionId(id)
            _sessionId.value = id
            _auth.value = true
        }
    }

    fun logout() = viewModelScope.launch {
        sessionId.value?.let {
            api.getStatus(it)
            prefs.clearSessionId()
            _sessionId.value = null
            _auth.value = false
        }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {
    val chats = repo.observeChats()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun load() = viewModelScope.launch {
        repo.refreshChats()
    }
}
