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
    object AwaitingRegistration : SessionUiState
    data class AwaitingScan(val sessionId: String) : SessionUiState
    object Authenticated : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repo: MessageRepository
) : ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs: StateFlow<List<Message>> = _msgs.asStateFlow()
    private var currentJid: String?
= null
    
    // This function now only clears the previous chat's messages
    fun prepareForJid(jid: String) {
        currentJid = jid
        _msgs.value = emptyList()
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        val tempId = UUID.randomUUID().toString()
        val optimisticMsg = Message(
            id = tempId, isOutgoing = true, text = text, jid = jid,
        
    timestamp = System.currentTimeMillis(),
            reactions = emptyMap(), status = MessageStatus.SENDING
        )
        _msgs.update { listOf(optimisticMsg) + it }

        // Backend no longer confirms message ID or timestamp, so we just fire and forget.
        // We can't reliably update the message status to SENT or FAILED.
        repo.sendText(sessionId, jid, text)
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val api: ApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val sessionId = prefs.sessionIdFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  
  fun start() = viewModelScope.launch {
        _uiState.value = SessionUiState.Loading
        val savedId = prefs.getSessionId()
        if (savedId == null) {
            _uiState.value = SessionUiState.AwaitingRegistration
            return@launch
        }
        
        // Check if saved session is still active
        val activeSessions = api.getSessions()
        if (activeSessions.contains(savedId)) {
            _uiState.value = SessionUiState.Authenticated
        } else {
            // Session expired or was deleted
            prefs.clearSessionId()
            _uiState.value = SessionUiState.AwaitingRegistration
        }
    }

    fun registerSession(sessionId: String) = viewModelScope.launch {
        _uiState.value = SessionUiState.Loading
        val success = api.createSession(sessionId)
        if (!success) {
            _uiState.value = SessionUiState.Error("Failed to register session. It might already exist.")
            delay(2000)
            _uiState.value = SessionUiState.AwaitingRegistration
            return@launch
        }

        prefs.saveSessionId(sessionId)
        _uiState.value = SessionUiState.AwaitingScan(sessionId)

        // Poll the server to see when the QR code is scanned and the session becomes active.
        while (true) {
            delay(3000)
            val activeSessions = api.getSessions()
            if (activeSessions.contains(sessionId)) {
                _uiState.value = SessionUiState.Authenticated
                return@launch
            }
            // Keep polling while in AwaitingScan state
            if (uiState.value !is SessionUiState.AwaitingScan) break
        }
    }

    fun logout() = viewModelScope.launch {
        prefs.clearSessionId()
        _uiState.value = SessionUiState.AwaitingRegistration
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