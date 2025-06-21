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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class ChatViewModel(
    app: Application,
    private val jid: String,
    private val contactName: String
) : AndroidViewModel(app) {

    private val repo = WhatsAppRepository(app)
    private val storage = MessageStorage(app)
    private val socket = ApiClient.getSocketManager(app)

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors = _errors.asSharedFlow()

    init {
        loadMessages()
        observeSocket()
    }
    
    // ++ NUEVA: Carga mensajes más antiguos al hacer "swipe to refresh"
    fun loadOlderMessages() = viewModelScope.launch {
        if (state.value.loadingOlder || state.value.messages.isEmpty()) return@launch

        _state.update { it.copy(loadingOlder = true) }
        val oldestTimestamp = state.value.messages.values.minOfOrNull { it.timestamp }

        if (oldestTimestamp == null) {
            _state.update { it.copy(loadingOlder = false) }
            return@launch
        }

        repo.getMessageHistory(jid, before = oldestTimestamp).onSuccess { olderMessages ->
            if (olderMessages.isNotEmpty()) {
                val currentMessages = state.value.messages
                val combined = (olderMessages + currentMessages.values).associateBy { it.id }
                storage.saveMessages(jid, combined.values.toList())
                _state.update { it.copy(messages = combined) }
            }
        }.onFailure {
            _errors.emit(it.message ?: "Failed to load older messages")
        }
        _state.update { it.copy(loadingOlder = false) }
    }

    private fun loadMessages() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val local = storage.getMessages(jid)
        _state.update { it.copy(messages = local.associateBy { it.id }) }

        repo.getMessageHistory(jid).onSuccess { server ->
            val combined = (server + local).associateBy { it.id }
            storage.saveMessages(jid, combined.values.toList())
            _state.update { it.copy(loading = false, messages = combined) }
            downloadMissingMedia(combined.values)
        }.onFailure {
            _state.update { it.copy(loading = false) }
            _errors.emit(it.message ?: "History error")
            downloadMissingMedia(local)
        }
    }

    private fun downloadMissingMedia(messages: Collection<Message>) {
        messages.forEach {
            if (it.mediaUrl != null && it.localMediaPath == null && it.mediaSha256 != null) {
                downloadMedia(it)
            }
        }
    }

    private fun downloadMedia(msg: Message) = viewModelScope.launch(Dispatchers.IO) {
        val ext = MediaCache.fileExt(msg.mimetype)
        val file = MediaCache.downloadAndCache(getApplication(), msg.mediaUrl!!, msg.mediaSha256!!, ext)
        file?.let {
            val path = it.absolutePath
            updateMessageInState(msg.id) { m -> m.copy(localMediaPath = path) }
            storage.updateMessageLocalPath(jid, msg.id, path)
        }
    }

    private fun observeSocket() {
        socket.incomingMessages
            .filter { it.jid == jid }
            .onEach {
                storage.addMessage(jid, it)
                addOrUpdateMessageInState(it.id, it)
                downloadMedia(it)
            }.launchIn(viewModelScope)

        socket.messageStatusUpdates
            .onEach {
                updateMessageInState(it.id) { m -> m.copy(status = it.status) }
                storage.updateMessageStatus(jid, it.id, it.status)
            }.launchIn(viewModelScope)
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val msg = Message(jid = jid, text = text, isOutgoing = true, senderName = "Me")
        addOrUpdateMessageInState(msg.id, msg)
        storage.addMessage(jid, msg)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, msg.id).onSuccess {
                val newId = it.messageId ?: msg.id
                storage.updateMessage(jid, msg.id, newId, "sent")
                replaceMessageInState(msg.id, msg.copy(id = newId, status = "sent"))
            }.onFailure {
                storage.updateMessageStatus(jid, msg.id, "failed")
                updateMessageInState(msg.id) { m -> m.copy(status = "failed") }
                _errors.emit("Send failed: ${it.message}")
            }
        }
    }

    fun sendMedia(uri: Uri) = viewModelScope.launch {
        val (file, sha) = MediaCache.saveToCache(getApplication(), uri) ?: run {
            _errors.emit("Media error"); return@launch
        }

        val mime = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"
        val msg = Message(
            jid = jid, text = null, isOutgoing = true, status = "sending",
            localMediaPath = file.absolutePath, mediaSha256 = sha, mimetype = mime, senderName = "Me"
        )
        addOrUpdateMessageInState(msg.id, msg)
        storage.addMessage(jid, msg)

        repo.sendMediaMessage(jid, msg.id, file, null).onSuccess {
            val newId = it.messageId ?: msg.id
            storage.updateMessage(jid, msg.id, newId, "sent")
            replaceMessageInState(msg.id, msg.copy(id = newId, status = "sent"))
        }.onFailure {
            storage.updateMessageStatus(jid, msg.id, "failed")
            updateMessageInState(msg.id) { m -> m.copy(status = "failed") }
            _errors.emit("Upload failed: ${it.message}")
        }
    }

    // ++ CORREGIDO: Se eliminó el parámetro `isOutgoing`
    fun sendReaction(msg: Message, emoji: String) = viewModelScope.launch {
        repo.sendReaction(jid, msg.id, emoji).onFailure {
            _errors.emit("Reaction failed")
        }
    }

    suspend fun getMediaFile(msg: Message): File? = withContext(Dispatchers.IO) {
        msg.localMediaPath?.let { path -> File(path).takeIf { it.exists() } }
    }

    private fun addOrUpdateMessageInState(id: String, message: Message) {
        _state.update {
            it.copy(messages = it.messages + (id to message))
        }
    }

    private fun updateMessageInState(id: String, edit: (Message) -> Message) {
        _state.update {
            val old = it.messages[id] ?: return@update it
            val updated = edit(old)
            it.copy(messages = it.messages + (id to updated))
        }
    }
    
    private fun replaceMessageInState(oldId: String, newMessage: Message) {
        _state.update {
            it.copy(messages = it.messages - oldId + (newMessage.id to newMessage))
        }
    }

    val visibleMessages: Flow<List<Message>> = state.map { it.messages.values.sortedBy { it.timestamp } }

    data class UiState(
        val loading: Boolean = false,
        val loadingOlder: Boolean = false, // ++ NUEVO: para el swipe refresh
        val messages: Map<String, Message> = emptyMap()
    )
}

class ChatViewModelFactory(
    private val app: Application,
    private val jid: String,
    private val name: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(app, jid, name) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
