// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ChatViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.data.WhatsAppRepository
import com.radwrld.wami.network.GroupInfo
import com.radwrld.wami.network.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.GroupStorage
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

data class UiState(
    val chatName: String = "",
    val loading: Boolean = false,
    val loadingOlder: Boolean = false,
    val messages: List<ChatListItem> = emptyList()
)

class ChatViewModel(
    app: Application,
    private val jid: String,

    private val contactStorage: ContactStorage,
    private val messageStorage: MessageStorage,
    private val whatsAppRepository: WhatsAppRepository
) : AndroidViewModel(app) {

    private val _internalState = MutableStateFlow(InternalState())
    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val chatDetailsFlow: Flow<String> = if (jid.endsWith("@g.us")) {
        flow {
            val groupInfoResult = whatsAppRepository.getGroupInfo(jid)
            val groupName = groupInfoResult.getOrNull()?.subject ?: ""
            emit(groupName)
        }
    } else {
        contactStorage.getContact(jid).map { it?.name ?: "Desconocido" }
    }

    val uiState: StateFlow<UiState> = combine(
        messageStorage.getMessagesFlow(jid),
        _internalState,
        chatDetailsFlow
    ) { messages, internalState, chatName ->
        UiState(
            chatName = chatName,
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

        whatsAppRepository.getMessageHistory(jid).onSuccess { olderMessages: List<Message> ->
            if (olderMessages.isNotEmpty()) {
                messageStorage.appendMessages(jid, olderMessages)
            }
        }.onFailure { e: Throwable ->
            _errors.emit(e.message ?: "Failed to load older messages")
        }

        _internalState.update { it.copy(loadingOlder = false) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val tempMessage = Message(id = "temp_${UUID.randomUUID()}", jid = jid, text = text, isOutgoing = true, status = "sending")
        viewModelScope.launch {
            messageStorage.addMessage(jid, tempMessage)
            val result = whatsAppRepository.sendTextMessage(jid, text, tempMessage.id)
            if (result.isSuccess) {
                messageStorage.updateMessageStatus(jid, tempMessage.id, "sent")
            } else {
                messageStorage.updateMessageStatus(jid, tempMessage.id, "failed")
                _errors.emit("Send failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendMedia(uri: Uri) = viewModelScope.launch {
        val (file, sha) = MediaCache.saveToCache(getApplication(), uri) ?: run {
            _errors.emit("Media error")
            return@launch
        }
        val mime = getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"

        val tempMessage = Message(
            id = "temp_${UUID.randomUUID()}", jid = jid, text = null, isOutgoing = true,
            status = "sending", localMediaPath = file.absolutePath, mediaSha256 = sha,
            mimetype = mime, name = "Me", senderName = "Me"
        )

        messageStorage.addMessage(jid, tempMessage)
        val result = whatsAppRepository.sendMediaMessage(jid, tempMessage.id, file, null)
        if (result.isSuccess) {
            messageStorage.updateMessageStatus(jid, tempMessage.id, "sent")
        } else {
            messageStorage.updateMessageStatus(jid, tempMessage.id, "failed")
            _errors.emit("Upload failed: ${result.exceptionOrNull()?.message}")
        }
    }

    fun sendReaction(messageId: String, emoji: String) = viewModelScope.launch {
        whatsAppRepository.sendReaction(jid, messageId, emoji).onFailure { e: Throwable ->
            _errors.emit("Reaction failed: ${e.message}")
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

    private data class InternalState(
        val loading: Boolean = false,
        val loadingOlder: Boolean = false
    )

    private fun shouldShowDivider(prev: Long, cur: Long): Boolean {
        if (prev == 0L) return true
        val gap = cur - prev
        return gap > TimeUnit.MINUTES.toMillis(30) ||
                isDiffDay(prev, cur)
    }

    private fun isDiffDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) ||
                c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
    }
}

class ChatViewModelFactory(
    private val app: Application,
    private val jid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                app = app,
                jid = jid,
                // Correctly get the singleton instance here.
                contactStorage = ContactStorage.getInstance(app),
                messageStorage = MessageStorage(app),
                whatsAppRepository = WhatsAppRepository(app)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}