// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ChatViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SendReactionRequest
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ChatViewModel(
    application: Application,
    private val jid: String,
    private val contactName: String
) : AndroidViewModel(application) {

    private val repo = WhatsAppRepository(application)
    private val messageStorage = MessageStorage(application)
    private val socketManager = ApiClient.getSocketManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAndSyncHistory()
        observeSocketEvents()
    }

    private fun loadAndSyncHistory() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true) }
            val localMessages = messageStorage.getMessages(jid)
            _uiState.update { state -> state.copy(messages = localMessages) }

            repo.getMessageHistory(jid)
                .onSuccess { serverMessages ->
                    val combined = (serverMessages + localMessages).distinctBy { message -> message.id }.sortedBy { message -> message.timestamp }
                    messageStorage.saveMessages(jid, combined)
                    _uiState.update { state -> state.copy(isLoading = false, messages = combined, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun observeSocketEvents() {
        socketManager.incomingMessages
            .filter { message -> message.jid == jid }
            .onEach { message ->
                val currentMessages = _uiState.value.messages
                if (currentMessages.none { msg -> msg.id == message.id }) {
                    val updatedMessages = (currentMessages + message).sortedBy { msg -> msg.timestamp }
                    messageStorage.addMessage(jid, message)
                    _uiState.update { state -> state.copy(messages = updatedMessages) }
                }
            }
            .launchIn(viewModelScope)

        socketManager.messageStatusUpdates
            .onEach { update ->
                updateMessageStatus(update.id, update.status)
            }
            .launchIn(viewModelScope)
    }

    fun sendTextMessage(text: String) {
        val tempId = UUID.randomUUID().toString()
        val message = Message(
            id = tempId,
            jid = jid,
            name = contactName,
            text = text,
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = (_uiState.value.messages + message).sortedBy { msg -> msg.timestamp }
        _uiState.update { state -> state.copy(messages = updatedMessages) }
        messageStorage.addMessage(jid, message)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, tempId)
                .onSuccess { response ->
                    val finalId = response.messageId ?: tempId
                    messageStorage.updateMessage(jid, tempId, finalId, "sent")
                    updateMessageStatus(tempId, "sent", newId = finalId)
                }
                .onFailure { error ->
                    messageStorage.updateMessageStatus(jid, tempId, "failed")
                    updateMessageStatus(tempId, "failed")
                    _uiState.update { state -> state.copy(error = "Failed to send: ${error.message}") }
                }
        }
    }

    fun sendMediaMessage(uri: Uri) {
        viewModelScope.launch {
            repo.sendMediaMessage(jid, uri, null)
                .onSuccess {
                    loadAndSyncHistory()
                }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(error = "Failed to send media: ${error.message}") }
                }
        }
    }
    
    fun sendReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            try {
                ApiClient.getInstance(getApplication()).sendReaction(
                    SendReactionRequest(
                        jid = message.jid,
                        messageId = message.id,
                        fromMe = message.isOutgoing,
                        emoji = emoji
                    )
                )
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(error = "Reaction failed") }
            }
        }
    }

    private fun updateMessageStatus(messageId: String, newStatus: String, newId: String? = null) {
        val currentMessages = _uiState.value.messages
        val messageIndex = currentMessages.indexOfFirst { message -> message.id == messageId }

        if (messageIndex != -1) {
            val updatedMessages = currentMessages.toMutableList()
            val originalMessage = updatedMessages[messageIndex]
            val updatedMessage = originalMessage.copy(
                status = newStatus,
                id = newId ?: originalMessage.id
            )
            updatedMessages[messageIndex] = updatedMessage
            _uiState.update { state -> state.copy(messages = updatedMessages) }
        }
    }

    data class UiState(
        val isLoading: Boolean = false,
        val messages: List<Message> = emptyList(),
        val error: String? = null
    )
}

class ChatViewModelFactory(
    private val application: Application,
    private val jid: String,
    private val contactName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, jid, contactName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
