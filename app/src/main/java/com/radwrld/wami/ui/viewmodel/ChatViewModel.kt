package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.radwrld.wami.data.MessageRepository
import com.radwrld.wami.network.Message
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class ChatViewModel(
    app: Application,
    private val jid: String,
    private val contactName: String
) : AndroidViewModel(app) {

    private val messageRepository = MessageRepository(getApplication())
    private val whatsAppRepository = WhatsAppRepository(getApplication())
    
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val visibleMessages: StateFlow<List<Message>> = messageRepository.getMessages(jid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {

    }

    fun loadOlderMessages() = viewModelScope.launch {
        if (_state.value.loadingOlder) return@launch
        _state.update { it.copy(loadingOlder = true) }

        val oldestTimestamp = visibleMessages.value.firstOrNull()?.timestamp
        
        whatsAppRepository.getMessageHistory(jid, before = oldestTimestamp).onSuccess { olderMessages ->
            if (olderMessages.isNotEmpty()) {

                messageRepository.appendMessages(jid, olderMessages)
            }
        }.onFailure {
            _errors.emit(it.message ?: "Failed to load older messages")
        }
        
        _state.update { it.copy(loadingOlder = false) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        
        val tempMessage = Message(
            id = "temp_${UUID.randomUUID()}",
            jid = jid,
            text = text,
            isOutgoing = true,
            status = "sending",
            timestamp = System.currentTimeMillis(),
            name = "Me",
            senderName = "Me"
        )

        viewModelScope.launch {
            messageRepository.addMessage(jid, tempMessage)
            
            whatsAppRepository.sendTextMessage(jid, text, tempMessage.id).onSuccess { response ->
                val newId = response.messageId ?: tempMessage.id
                messageRepository.updateMessage(jid, tempMessage.id, newId, "sent")
            }.onFailure {
                messageRepository.updateMessageStatus(jid, tempMessage.id, "failed")
                _errors.emit("Send failed: ${it.message}")
            }
        }
    }

    fun sendMedia(uri: Uri) = viewModelScope.launch {
        val (file, sha) = MediaCache.saveToCache(getApplication(), uri) ?: run {
            _errors.emit("Media error"); return@launch
        }
        val mime = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"
        
        val tempMessage = Message(
            id = "temp_${UUID.randomUUID()}",
            jid = jid, text = null, isOutgoing = true, status = "sending",
            localMediaPath = file.absolutePath, mediaSha256 = sha, mimetype = mime, name = "Me", senderName = "Me"
        )

        messageRepository.addMessage(jid, tempMessage)

        whatsAppRepository.sendMediaMessage(jid, tempMessage.id, file, null).onSuccess { response ->
            val newId = response.messageId ?: tempMessage.id
            messageRepository.updateMessage(jid, tempMessage.id, newId, "sent")
        }.onFailure {
            messageRepository.updateMessageStatus(jid, tempMessage.id, "failed")
            _errors.emit("Upload failed: ${it.message}")
        }
    }

    fun sendReaction(msg: Message, emoji: String) = viewModelScope.launch {
        whatsAppRepository.sendReaction(jid, msg.id, emoji).onFailure {
            _errors.emit("Reaction failed")
        }
    }

    suspend fun getMediaFile(msg: Message): File? {
        msg.localMediaPath?.let { path ->
            File(path).takeIf { it.exists() }?.let { return it }
        }

        if (msg.mediaUrl.isNullOrBlank() || msg.mimetype.isNullOrBlank()) {
            return null
        }
        
        val ext = MediaCache.fileExt(msg.mimetype)
        val cacheKey = msg.mediaSha256 ?: msg.id

        val file = withContext(Dispatchers.IO) {
             MediaCache.downloadAndCache(
                context = getApplication(),
                url = msg.mediaUrl!!,
                cacheKey = cacheKey,
                ext = ext
            )
        }

        if (file != null) {
            messageRepository.updateMessageLocalPath(jid, msg.id, file.absolutePath)
        }

        return file
    }

    data class UiState(
        val loading: Boolean = false,
        val loadingOlder: Boolean = false
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



