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
    val msgs: StateFlow<List<Message>> = _msgs.asStateFlow()
    private var currentJid: String? = null

    fun load(jid: String) = viewModelScope.launch {
        currentJid = jid
        _msgs.value = repo.refreshMessages(jid)
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        val tempId = UUID.randomUUID().toString()
        val optimisticMsg = Message(
            id = tempId, isOutgoing = true, text = text, jid = jid,
            timestamp = System.currentTimeMillis(),
            reactions = emptyMap(), status = MessageStatus.SENDING
        )
        _msgs.update { listOf(optimisticMsg) + it }

        val response = repo.sendText(sessionId, jid, text, tempId)

        if (response != null) {
            _msgs.update { currentMsgs ->
                currentMsgs.map { msg ->
                    if (msg.id == tempId) {
                        msg.copy(
                            id = response.messageId,
                            timestamp = response.timestamp,
                            status = MessageStatus.SENT
                        )
                    } else {
                        msg
                    }
                }
            }
            repo.refreshMessages(jid)
        } else {
            _msgs.update {
                it.map { m -> if (m.id == tempId) m.copy(status = MessageStatus.FAILED) else m }
            }
        }
    }

    fun sendReaction(sessionId: String, jid: String, messageId: String, emoji: String) = viewModelScope.launch {
        val success = repo.sendReaction(sessionId, jid, messageId, emoji)
        if (success) {
            currentJid?.let {
                _msgs.value = repo.refreshMessages(it)
            }
        }
    }

    fun retryFailedMessage(message: Message, sessionId: String) = viewModelScope.launch {
        if (message.status != MessageStatus.FAILED || message.text == null || currentJid == null) return@launch

        _msgs.update { currentMsgs ->
            currentMsgs.map { msg ->
                if (msg.id == message.id) {
                    msg.copy(status = MessageStatus.SENDING)
                } else {
                    msg
                }
            }
        }

        val response = repo.sendText(sessionId, currentJid!!, message.text, message.id)

        if (response != null) {
            _msgs.update { currentMsgs ->
                currentMsgs.map { msg ->
                    if (msg.id == message.id) {
                        msg.copy(
                            id = response.messageId,
                            timestamp = response.timestamp,
                            status = MessageStatus.SENT
                        )
                    } else {
                        msg
                    }
                }
            }
            repo.refreshMessages(currentJid!!)
        } else {
            _msgs.update {
                it.map { m -> if (m.id == message.id) m.copy(status = MessageStatus.FAILED) else m }
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
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

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
        val id = api.createSession()
        if (id == null) {
            _uiState.value = SessionUiState.Error("Failed to create session")
            return@launch
        }

        prefs.saveSessionId(id)

        while (true) {
            delay(3000)
            val status = api.getStatus(id)
            if (status == null) {
                _uiState.value = SessionUiState.Error("Failed to get status")
                return@launch
            }
            if (status.connected) {
                _uiState.value = SessionUiState.Authenticated
                return@launch
            }
            _uiState.value = SessionUiState.AwaitingScan(status.qr)
        }
    }

    fun loginWithId(id: String) = viewModelScope.launch {
        _uiState.value = SessionUiState.Loading
        val status = api.getStatus(id)
        if (status?.connected == true) {
            prefs.saveSessionId(id)
            _uiState.value = SessionUiState.Authenticated
        } else {
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
