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
        // FIXED: Renamed MediaCache.getFileExtensionFromMimeType to MediaCache.fileExt
        val ext = MediaCache.fileExt(msg.mimetype)
        val file = MediaCache.downloadAndCache(getApplication(), msg.mediaUrl!!, msg.mediaSha256!!, ext)
        file?.let {
            val path = it.absolutePath
            update(msg.id) { m -> m.copy(localMediaPath = path) }
            storage.updateMessageLocalPath(jid, msg.id, path)
        }
    }

    private fun observeSocket() {
        socket.incomingMessages
            .filter { it.jid == jid }
            .onEach {
                storage.addMessage(jid, it)
                update(it.id) { it }
                downloadMedia(it)
            }.launchIn(viewModelScope)

        socket.messageStatusUpdates
            .onEach {
                update(it.id) { m -> m.copy(status = it.status) }
                storage.updateMessageStatus(jid, it.id, it.status)
            }.launchIn(viewModelScope)
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val msg = Message(jid = jid, text = text, isOutgoing = true, senderName = "Me")
        update(msg.id) { msg }
        storage.addMessage(jid, msg)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, msg.id).onSuccess {
                val newId = it.messageId ?: msg.id
                storage.updateMessage(jid, msg.id, newId, "sent")
                update(msg.id) { m -> m.copy(id = newId, status = "sent") }
            }.onFailure {
                storage.updateMessageStatus(jid, msg.id, "failed")
                update(msg.id) { m -> m.copy(status = "failed") }
                _errors.emit("Send failed: ${it.message}")
            }
        }
    }

    fun sendMedia(uri: Uri) = viewModelScope.launch {
        val (file, sha) = MediaCache.saveToCache(getApplication(), uri) ?: run {
            _errors.emit("Media error"); return@launch
        }

        val mime = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"
        // FIXED: Used named arguments for the Message constructor to prevent type mismatches.
        val msg = Message(
            jid = jid,
            text = null,
            isOutgoing = true,
            status = "sending",
            localMediaPath = file.absolutePath,
            mediaSha256 = sha,
            mimetype = mime,
            senderName = "Me"
        )
        update(msg.id) { msg }
        storage.addMessage(jid, msg)

        repo.sendMediaMessage(jid, msg.id, file, null).onSuccess {
            val newId = it.messageId ?: msg.id
            storage.updateMessage(jid, msg.id, newId, "sent")
            update(msg.id) { m -> m.copy(id = newId, status = "sent") }
        }.onFailure {
            storage.updateMessageStatus(jid, msg.id, "failed")
            update(msg.id) { m -> m.copy(status = "failed") }
            _errors.emit("Upload failed: ${it.message}")
        }
    }

    fun sendReaction(msg: Message, emoji: String) = viewModelScope.launch {
        repo.sendReaction(jid, msg.id, msg.isOutgoing, emoji).onFailure {
            _errors.emit("Reaction failed")
        }
    }

    suspend fun getMediaFile(msg: Message): File? = withContext(Dispatchers.IO) {
        msg.localMediaPath?.let { path -> File(path).takeIf { it.exists() } }
    }

    private fun update(id: String, edit: (Message) -> Message) {
        _state.update {
            val old = it.messages[id] ?: return
            val updated = edit(old)
            it.copy(messages = it.messages - id + (updated.id to updated))
        }
    }

    val visibleMessages: Flow<List<Message>> = state.map { it.messages.values.sortedBy { it.timestamp } }

    data class UiState(
        val loading: Boolean = false,
        val messages: Map<String, Message> = emptyMap()
    )
}

class ChatViewModelFactory(
    private val app: Application,
    private val jid: String,
    private val name: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is assignable from ChatViewModel
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // Suppress the "UNCHECKED_CAST" warning as we have verified the class type
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(app, jid, name) as T
        }
        // If it's an unknown class, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
