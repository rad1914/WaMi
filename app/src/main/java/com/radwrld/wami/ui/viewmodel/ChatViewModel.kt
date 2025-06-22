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

    // ++ CORREGIDO: Simplificado para usar la nueva lógica de getMediaFile
    private fun downloadMissingMedia(messages: Collection<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.forEach { msg ->
                if (msg.mediaUrl != null && msg.localMediaPath == null) {
                    // Simplemente llama a getMediaFile, que ahora maneja toda la lógica.
                    // No es necesario verificar el resultado aquí, solo queremos iniciar la descarga.
                    getMediaFile(msg)
                }
            }
        }
    }

    // Se eliminó la función `downloadMedia` anterior, ya que su lógica ahora está en `getMediaFile`.

    private fun observeSocket() {
        socket.incomingMessages
            .filter { it.jid == jid }
            .onEach { msg ->
                storage.addMessage(jid, msg)
                addOrUpdateMessageInState(msg.id, msg)
                // ++ CORREGIDO: Llama a getMediaFile para descargar automáticamente el medio del nuevo mensaje.
                if (msg.mediaUrl != null) {
                    getMediaFile(msg)
                }
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

    fun sendReaction(msg: Message, emoji: String) = viewModelScope.launch {
        repo.sendReaction(jid, msg.id, emoji).onFailure {
            _errors.emit("Reaction failed")
        }
    }

    // ++ CORREGIDO: Esta es la función clave. Ahora maneja la lógica de caché y descarga.
    suspend fun getMediaFile(msg: Message): File? {
        // 1. Revisa si ya existe un archivo local válido.
        msg.localMediaPath?.let { path ->
            File(path).takeIf { it.exists() }?.let { return it }
        }

        // 2. Verifica si se puede descargar.
        if (msg.mediaUrl.isNullOrBlank() || msg.mimetype.isNullOrBlank()) {
            return null
        }
        
        val ext = MediaCache.fileExt(msg.mimetype)

        // 3. Usa el hash SHA256 como clave si está disponible; si no, usa el ID del mensaje.
        val cacheKey = msg.mediaSha256 ?: msg.id

        // 4. Llama a MediaCache. Esta función descargará el archivo si no está en caché.
        val file = withContext(Dispatchers.IO) {
             MediaCache.downloadAndCache(
                context = getApplication(),
                url = msg.mediaUrl!!,
                cacheKey = cacheKey,
                ext = ext
            )
        }

        // 5. Si la descarga fue exitosa, actualiza el estado y el almacenamiento local.
        if (file != null) {
            val path = file.absolutePath
            updateMessageInState(msg.id) { m -> m.copy(localMediaPath = path) }
            storage.updateMessageLocalPath(jid, msg.id, path)
        }

        return file
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
        val loadingOlder: Boolean = false,
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
