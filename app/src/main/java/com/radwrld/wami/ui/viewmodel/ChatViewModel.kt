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
    
    private fun Map<String, Message>.toSortedList(): List<Message> {
        return this.values.sortedBy { it.timestamp }
    }

    private fun loadAndSyncHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Load local messages first for instant display
            val localMessages = messageStorage.getMessages(jid)
            _uiState.update { it.copy(messages = localMessages.associateBy { m -> m.id }) }
            
            // Sync with server
            repo.getMessageHistory(jid)
                .onSuccess { serverMessages ->
                    val combinedMap = (serverMessages.associateBy { it.id } + localMessages.associateBy { it.id })
                    messageStorage.saveMessages(jid, combinedMap.values.toList())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = combinedMap,
                        )
                    }
                    // ++ After loading history, check for any media that needs to be downloaded.
                    triggerBackgroundDownloads(combinedMap.values.toList())
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, messages = localMessages.associateBy { m -> m.id }) }
                    _errorEvents.emit(error.message ?: "Failed to load history")
                    triggerBackgroundDownloads(localMessages) // Still try to download from local data
                }
        }
    }
    
    // ++ This function triggers the background download for a list of messages.
    private fun triggerBackgroundDownloads(messages: List<Message>) {
        messages.forEach { msg ->
            if (msg.mediaUrl != null && msg.localMediaPath == null && msg.mediaSha256 != null) {
                downloadMediaForMessage(msg)
            }
        }
    }

    // ++ This is the core background download logic for a single message.
    private fun downloadMediaForMessage(message: Message) {
        // Avoid re-downloading if already in progress or done.
        if (message.mediaUrl == null || message.mediaSha256 == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val extension = MediaCache.getFileExtensionFromMimeType(message.mimetype)
            val cachedFile = MediaCache.downloadAndCache(getApplication(), message.mediaUrl, message.mediaSha256, extension)

            if (cachedFile != null) {
                // Once downloaded, update the message in the UI state and local storage.
                val localPath = cachedFile.absolutePath
                updateMessage(message.id) { it.copy(localMediaPath = localPath) }
                messageStorage.updateMessageLocalPath(jid, message.id, localPath)
            }
        }
    }

    private fun observeSocketEvents() {
        socketManager.incomingMessages
            .filter { message -> message.jid == jid }
            .onEach { message ->
                messageStorage.addMessage(jid, message)
                addOrUpdateMessage(message)
                // ++ Also trigger download for new messages arriving via socket.
                downloadMediaForMessage(message)
            }
            .launchIn(viewModelScope)

        socketManager.messageStatusUpdates
            .onEach { update ->
                updateMessage(update.id) { it.copy(status = update.status) }
                messageStorage.updateMessageStatus(jid, update.id, update.status)
            }
            .launchIn(viewModelScope)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val message = Message(
            jid = jid,
            text = text,
            isOutgoing = true,
            senderName = "Me"
        )
        addOrUpdateMessage(message)
        messageStorage.addMessage(jid, message)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, message.id)
                .onSuccess { response ->
                    val finalId = response.messageId ?: message.id
                    messageStorage.updateMessage(jid, message.id, finalId, "sent")
                    updateMessage(message.id) { it.copy(id = finalId, status = "sent") }
                }
                .onFailure { error ->
                    messageStorage.updateMessageStatus(jid, message.id, "failed")
                    updateMessage(message.id) { it.copy(status = "failed") }
                    _errorEvents.emit("Failed to send: ${error.message}")
                }
        }
    }

    fun sendMediaMessage(uri: Uri) {
        viewModelScope.launch {
            // ++ 1. Immediately save the media to our local cache.
            val cacheResult = MediaCache.saveToCache(getApplication(), uri)
            if (cacheResult == null) {
                _errorEvents.emit("Failed to process media file.")
                return@launch
            }
            val (cachedFile, sha256) = cacheResult
            
            // ++ 2. Create the message with the local path and hash already populated.
            val mimeType = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"
            val message = Message(
                jid = jid,
                text = null,
                isOutgoing = true,
                status = "sending",
                localMediaPath = cachedFile.absolutePath, // Use the new local path
                mediaSha256 = sha256, // Use the calculated hash
                mimetype = mimeType,
                senderName = "Me"
            )

            // ++ 3. Add to UI and storage instantly. The preview will now work immediately.
            addOrUpdateMessage(message)
            messageStorage.addMessage(jid, message)

            // ++ 4. Start the upload in the background.
            repo.sendMediaMessage(jid, message.id, cachedFile, null)
                .onSuccess { response ->
                    val finalId = response.messageId ?: message.id
                    messageStorage.updateMessage(jid, message.id, finalId, "sent")
                    updateMessage(message.id) { it.copy(id = finalId, status = "sent") }
                }
                .onFailure { error ->
                    messageStorage.updateMessageStatus(jid, message.id, "failed")
                    updateMessage(message.id) { it.copy(status = "failed") }
                    _errorEvents.emit("Upload failed: ${error.message}")
                }
        }
    }

    // ++ This function becomes very simple: it just finds the file that should already be cached.
    suspend fun getMediaFile(message: Message): File? = withContext(Dispatchers.IO) {
        message.localMediaPath?.let { path ->
            val file = File(path)
            if (file.exists()) file else null
        }
    }

    fun sendReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            repo.sendReaction(jid = message.jid, messageId = message.id, fromMe = message.isOutgoing, emoji = emoji)
                .onFailure { _errorEvents.emit("Reaction failed") }
        }
    }

    private fun addOrUpdateMessage(message: Message) {
        _uiState.update { state -> state.copy(messages = state.messages + (message.id to message)) }
    }

    private fun updateMessage(messageId: String, transformation: (Message) -> Message) {
        _uiState.update { state ->
            state.messages[messageId]?.let { originalMessage ->
                val transformed = transformation(originalMessage)
                val newMap = state.messages - messageId + (transformed.id to transformed)
                state.copy(messages = newMap)
            } ?: state
        }
    }

    // Use a Flow to continuously provide the sorted list of messages to the UI.
    val visibleMessagesFlow: Flow<List<Message>> = uiState.map { it.messages.toSortedList() }

    data class UiState(
        val isLoading: Boolean = false,
        val messages: Map<String, Message> = emptyMap()
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
