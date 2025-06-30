// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ChatViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.network.Message
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class ChatViewModel(
    app: Application,
    private val jid: String,
    private val contactName: String
) : AndroidViewModel(app) {

    private val messageStorage = MessageStorage(getApplication())
    private val whatsAppRepository = WhatsAppRepository(getApplication())

    private val _internalState = MutableStateFlow(InternalState())
    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val uiState: StateFlow<UiState> = messageStorage.getMessagesFlow(jid)
        .combine(_internalState) { messages, internalState ->
            UiState(
                loading = internalState.loading,
                loadingOlder = internalState.loadingOlder,
                messages = processMessagesIntoListItems(messages)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState()
        )

    private fun processMessagesIntoListItems(messages: List<Message>): List<ChatListItem> {
        return buildList {
            add(ChatListItem.WarningItem)
            var lastTs = 0L
            messages.forEach { m ->
                if (shouldShowDivider(lastTs, m.timestamp)) {
                    add(ChatListItem.DividerItem(m.timestamp))
                }
                add(ChatListItem.MessageItem(m))
                lastTs = m.timestamp
            }
        }
    }

    fun loadOlderMessages() = viewModelScope.launch {
        if (_internalState.value.loadingOlder) return@launch
        _internalState.update { it.copy(loadingOlder = true) }

        whatsAppRepository.getMessageHistory(jid).onSuccess { olderMessages ->
            if (olderMessages.isNotEmpty()) {
                messageStorage.appendMessages(jid, olderMessages)
            }
        }.onFailure {
            _errors.emit(it.message ?: "Failed to load older messages")
        }
        
        _internalState.update { it.copy(loadingOlder = false) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val tempMessage = Message(id = "temp_${UUID.randomUUID()}", jid = jid, text = text, isOutgoing = true, status = "sending")
        viewModelScope.launch {
            messageStorage.addMessage(jid, tempMessage)
            whatsAppRepository.sendTextMessage(jid, text, tempMessage.id).onSuccess { response ->
                val newId = response.messageId ?: tempMessage.id
                messageStorage.updateMessage(jid, tempMessage.id, newId, "sent")
            }.onFailure {
                messageStorage.updateMessageStatus(jid, tempMessage.id, "failed")
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
            id = "temp_${UUID.randomUUID()}", jid = jid, text = null, isOutgoing = true,
            status = "sending", localMediaPath = file.absolutePath, mediaSha256 = sha,
            mimetype = mime, name = "Me", senderName = "Me"
        )

        messageStorage.addMessage(jid, tempMessage)
        whatsAppRepository.sendMediaMessage(jid, tempMessage.id, file, null).onSuccess { response ->
            val newId = response.messageId ?: tempMessage.id
            messageStorage.updateMessage(jid, tempMessage.id, newId, "sent")
        }.onFailure {
            messageStorage.updateMessageStatus(jid, tempMessage.id, "failed")
            _errors.emit("Upload failed: ${it.message}")
        }
    }

    fun sendReaction(messageId: String, emoji: String) = viewModelScope.launch {
        whatsAppRepository.sendReaction(jid, messageId, emoji).onFailure {
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
                context = getApplication(), url = msg.mediaUrl!!,
                cacheKey = cacheKey, ext = ext
            )
        }
        if (file != null) {
            messageStorage.updateMessageLocalPath(jid, msg.id, file.absolutePath)
        }
        return file
    }

    data class UiState(
        val loading: Boolean = false,
        val loadingOlder: Boolean = false,
        val messages: List<ChatListItem> = emptyList()
    )

    private data class InternalState(
        val loading: Boolean = false,
        val loadingOlder: Boolean = false
    )

    private fun shouldShowDivider(prev: Long, cur: Long): Boolean {
        if (prev == 0L) return true
        val gap = cur - prev
        return gap > TimeUnit.MINUTES.toMillis(30) || isDiffDay(prev, cur)
    }

    private fun isDiffDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) || c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
    }
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