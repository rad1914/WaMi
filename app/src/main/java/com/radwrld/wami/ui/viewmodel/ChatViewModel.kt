package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.SocketManager
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)
    private val messageStorage = MessageStorage(application)
    private val socketManager = SocketManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        listenForIncomingMessages()
        listenForStatusUpdates()
        socketManager.connect()
    }

    fun loadMessages(jid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, jid = jid) }
            val result = repository.getMessageHistory(jid)
            result.onSuccess { messages ->
                _uiState.update { it.copy(isLoading = false, messages = messages) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun sendTextMessage(text: String) {
        val jid = _uiState.value.jid ?: return
        // The corrected Message model now uses default values for timestamp and status
        val tempMessage = Message(jid = jid, text = text, isOutgoing = true)

        // Optimistically update the UI
        _uiState.update { it.copy(messages = it.messages + tempMessage) }
        messageStorage.addMessage(jid, tempMessage)

        viewModelScope.launch {
            val result = repository.sendTextMessage(jid, text, tempMessage.id)
            
            // UPDATED: Handle the success case to get the final messageId
            result.onSuccess { response ->
                val finalId = response.messageId ?: tempMessage.id
                val tempId = response.tempId ?: tempMessage.id

                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(id = finalId, status = "sent") else msg
                    })
                }
                messageStorage.updateMessage(jid, tempId, finalId, "sent")
            }
            
            result.onFailure {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempMessage.id) msg.copy(status = "failed") else msg
                    })
                }
                messageStorage.updateMessageStatus(jid, tempMessage.id, "failed")
            }
        }
    }

    fun sendMediaMessage(fileUri: Uri, caption: String?) {
        val jid = _uiState.value.jid ?: return
        viewModelScope.launch {
            repository.sendMediaMessage(jid, fileUri, caption)
        }
    }

    private fun listenForIncomingMessages() {
        socketManager.incomingMessages
            .onEach { message ->
                if (message.jid == _uiState.value.jid) {
                    _uiState.update {
                        if (it.messages.any { m -> m.id == message.id }) it
                        else it.copy(messages = it.messages + message)
                    }
                    messageStorage.addMessage(message.jid, message)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * UPDATED: This function no longer looks for 'tempId'. It only handles status updates
     * for messages that already have their final ID from the server.
     */
    private fun listenForStatusUpdates() {
        socketManager.messageStatusUpdates
            .onEach { update ->
                val jid = _uiState.value.jid ?: return@onEach
                
                _uiState.update { state ->
                    state.copy(messages = state.messages.map {
                        if (it.id == update.id) it.copy(status = update.status) else it
                    })
                }
                messageStorage.updateMessageStatus(jid, update.id, update.status)
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    data class ChatUiState(
        val isLoading: Boolean = false,
        val jid: String? = null,
        val messages: List<Message> = emptyList(),
        val error: String? = null
    )
}

