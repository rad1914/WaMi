// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ChatViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    init {
        loadAndSyncHistory()
        observeSocketEvents()
    }

    private fun getVisibleMessages(messages: List<Message>): List<Message> {
        return messages.filter { message ->
            !message.text.isNullOrBlank() ||
            !message.mediaUrl.isNullOrBlank() ||
            !message.localMediaPath.isNullOrBlank() ||
            !message.quotedMessageText.isNullOrBlank()
        }
    }
    
    private fun Map<String, Message>.toSortedList(): List<Message> {
        return this.values.sortedBy { it.timestamp }
    }

    private fun loadAndSyncHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val localMessages = messageStorage.getMessages(jid)
            val localMessageMap = localMessages.associateBy { it.id }
            val initialList = localMessageMap.toSortedList()
            _uiState.update {
                it.copy(
                    messages = localMessageMap,
                    visibleMessages = getVisibleMessages(initialList)
                )
            }

            repo.getMessageHistory(jid)
                .onSuccess { serverMessages ->
                    val combinedList = withContext(Dispatchers.Default) {
                        // Combine server and local, ensuring no duplicates, and sort
                        (serverMessages.associateBy { it.id } + localMessageMap).toSortedList()
                    }
                    messageStorage.saveMessages(jid, combinedList)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = combinedList.associateBy { m -> m.id },
                            visibleMessages = getVisibleMessages(combinedList)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _errorEvents.emit(error.message ?: "Failed to load history")
                }
        }
    }

    private fun observeSocketEvents() {
        socketManager.incomingMessages
            .filter { message -> message.jid == jid }
            .onEach { message ->
                messageStorage.addMessage(jid, message)
                addOrUpdateMessage(message)
            }
            .launchIn(viewModelScope)

        socketManager.messageStatusUpdates
            .onEach { update ->
                val finalId = update.id
                // Assuming status updates might refer to a tempId, which we don't have here.
                // This logic might need adjustment based on how socket updates work with temp IDs.
                updateMessage(finalId) { it.copy(status = update.status) }
                messageStorage.updateMessageStatus(jid, finalId, update.status)
            }
            .launchIn(viewModelScope)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return

        val tempId = UUID.randomUUID().toString()
        val message = Message(
            id = tempId,
            jid = jid,
            name = contactName,
            text = text,
            isOutgoing = true,
            timestamp = System.currentTimeMillis(),
            status = "sending"
        )

        addOrUpdateMessage(message)
        messageStorage.addMessage(jid, message)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, tempId)
                .onSuccess { response ->
                    val finalId = response.messageId ?: tempId
                    messageStorage.updateMessage(jid, tempId, finalId, "sent")
                    updateMessage(tempId) { it.copy(id = finalId, status = "sent") }
                }
                .onFailure { error ->
                    messageStorage.updateMessageStatus(jid, tempId, "failed")
                    updateMessage(tempId) { it.copy(status = "failed") }
                    _errorEvents.emit("Failed to send: ${error.message}")
                }
        }
    }

    fun sendMediaMessage(uri: Uri) {
        val tempId = UUID.randomUUID().toString()
        val mimeType = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"

        val tempMessage = Message(
            id = tempId,
            jid = jid,
            text = null,
            isOutgoing = true,
            status = "sending",
            timestamp = System.currentTimeMillis(),
            localMediaPath = uri.toString(),
            mimetype = mimeType
        )
        addOrUpdateMessage(tempMessage)
        messageStorage.addMessage(jid, tempMessage)

        viewModelScope.launch {
            val cacheResult = MediaCache.saveToCache(getApplication(), uri)

            if (cacheResult == null) {
                updateMessage(tempId) { it.copy(status = "failed") }
                messageStorage.updateMessageStatus(jid, tempId, "failed")
                _errorEvents.emit("Failed to cache media file.")
                return@launch
            }

            val (cachedFile, sha256) = cacheResult
            updateMessage(tempId) { it.copy(localMediaPath = cachedFile.absolutePath, mediaSha256 = sha256) }

            repo.sendMediaMessage(jid, tempId, cachedFile, null)
                .onSuccess { response ->
                    val finalId = response.messageId ?: tempId
                    updateMessage(tempId) { it.copy(id = finalId, status = "sent") }
                    messageStorage.updateMessage(jid, tempId, finalId, "sent")
                }
                .onFailure { error ->
                    updateMessage(tempId) { it.copy(status = "failed") }
                    messageStorage.updateMessageStatus(jid, tempId, "failed")
                    _errorEvents.emit("Upload failed: ${error.message}")
                }
        }
    }

    suspend fun getMediaFile(message: Message): File? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        message.localMediaPath?.let { path ->
            val file = File(path)
            if (file.exists()) return@withContext file
        }

        val hash = message.mediaSha256
        val url = message.mediaUrl
        if (hash != null && url != null) {
            val extension = MediaCache.getFileExtensionFromMimeType(message.mimetype)
            return@withContext MediaCache.downloadAndCache(context, url, hash, extension)
        }
        return@withContext null
    }

    fun sendReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            repo.sendReaction(jid = message.jid, messageId = message.id, fromMe = message.isOutgoing, emoji = emoji)
                .onFailure {
                     viewModelScope.launch { _errorEvents.emit("Reaction failed") }
                }
        }
    }

    private fun addOrUpdateMessage(message: Message) {
        _uiState.update { state ->
            val newMap = state.messages + (message.id to message)
            state.copy(messages = newMap, visibleMessages = getVisibleMessages(newMap.toSortedList()))
        }
    }

    private fun updateMessage(messageId: String, transformation: (Message) -> Message) {
        _uiState.update { state ->
            state.messages[messageId]?.let { originalMessage ->
                val transformed = transformation(originalMessage)
                val newMap = state.messages - messageId + (transformed.id to transformed)
                state.copy(messages = newMap, visibleMessages = getVisibleMessages(newMap.toSortedList()))
            } ?: state // If message not found, return original state
        }
    }

    data class UiState(
        val isLoading: Boolean = false,
        val messages: Map<String, Message> = emptyMap(),
        val visibleMessages: List<Message> = emptyList(),
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
