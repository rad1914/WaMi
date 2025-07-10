// @path: app/src/main/java/com/radwrld/wami/ui/vm/WaMiViewModels.kt
package com.radwrld.wami.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import com.radwrld.wami.data.ChatRepository
import com.radwrld.wami.data.MessageRepository
import com.radwrld.wami.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repo: MessageRepository
) : ViewModel() {
    private val _msgs = MutableStateFlow<List<com.radwrld.wami.data.Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(jid: String) = viewModelScope.launch {
        repo.observeMessages(jid)
            .firstOrNull()
            ?.let { _msgs.value = it }
        val remote = repo.refreshMessages(jid)
        _msgs.value = remote
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        val success = repo.sendText(sessionId, jid, text)
        if (success) {
            val msg = com.radwrld.wami.data.Message(
                id = UUID.randomUUID().toString(),
                fromMe = true,
                text = text,
                timestamp = System.currentTimeMillis(),
                jid = jid,
                reactions = ""
            )
            _msgs.value = listOf(msg) + _msgs.value
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
    val qrCode   = _qrCode.asStateFlow()
    val isAuth   = _auth.asStateFlow()

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
        api.createSession()?.also { id ->
            prefs.saveSessionId(id)
            _sessionId.value = id
            while (!isAuth.value) {
                delay(3000)
                api.getStatus(id)?.let {
                    _auth.value = it.connected
                    _qrCode.value = it.qr
                }
            }
        }
    }

    fun loginWithId(id: String) = viewModelScope.launch {
        if (api.getStatus(id)?.connected == true) {
            prefs.saveSessionId(id)
            _sessionId.value = id
            _auth.value = true
        }
    }

    fun logout() = viewModelScope.launch {
        sessionId.value?.let { id ->
            api.getStatus(id)
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
