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

sealed interface SessionUiState {
    object Loading : SessionUiState
    data class AwaitingScan(val qrCode: String?) : SessionUiState
    object Authenticated : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repo: MessageRepository
) : ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(jid: String) = viewModelScope.launch {
        _msgs.value = repo.refreshMessages(jid)
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        val tempId = UUID.randomUUID().toString()
        val optimisticMessage = Message(
            id = tempId,
            fromMe = true,
            text = text,
            timestamp = System.currentTimeMillis(),
            jid = jid,
            status = MessageStatus.SENDING
        )
        
        _msgs.update { listOf(optimisticMessage) + it }
        
        val success = repo.sendText(sessionId, jid, text, tempId)
        
        // TODO: Update message status based on server response
        if (success) {
            // Here you would ideally get the real message ID from the server 
            // and update the message in the list. For now, we just refresh.
            repo.refreshMessages(jid)
        } else {
             _msgs.update { currentMsgs ->
                currentMsgs.map { if (it.id == tempId) it.copy(status = MessageStatus.FAILED) else it }
            }
        }
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val api: ApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val sessionId = prefs.sessionIdFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun start() = viewModelScope.launch {
        _uiState.value = SessionUiState.Loading
        prefs.sessionIdFlow.firstOrNull()?.let { id ->
            if (api.getStatus(id)?.connected == true) {
                _uiState.value = SessionUiState.Authenticated
                return@launch
            }
        }
        createSession()
    }

    private fun createSession() = viewModelScope.launch {
        api.createSession()?.let { id ->
            prefs.saveSessionId(id)
            var authenticated = false
            while (!authenticated) {
                delay(3000)
                api.getStatus(id)?.let {
                    if (it.connected) {
                        authenticated = true
                        _uiState.value = SessionUiState.Authenticated
                    } else {
                        _uiState.value = SessionUiState.AwaitingScan(it.qr)
                    }
                } ?: run {
                    _uiState.value = SessionUiState.Error("Failed to get status")
                    return@launch
                }
            }
        } ?: run {
            _uiState.value = SessionUiState.Error("Failed to create session")
        }
    }

    fun loginWithId(id: String) = viewModelScope.launch {
        _uiState.value = SessionUiState.Loading
        api.getStatus(id)?.takeIf { it.connected }?.let {
            prefs.saveSessionId(id)
            _uiState.value = SessionUiState.Authenticated
        } ?: run {
            _uiState.value = SessionUiState.Error("Invalid or expired Session ID")
            delay(2000)
            start()
        }
    }

    fun logout() = viewModelScope.launch {
        prefs.clearSessionId()
        _uiState.value = SessionUiState.Loading
        start()
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