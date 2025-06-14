// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ChatViewModel.kt
// ChatViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.SocketManager
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = WhatsAppRepository(app)
    private val store = MessageStorage(app)
    private val sock  = SocketManager(app)

    private val _ui = MutableStateFlow(Ui())
    val ui         = _ui.asStateFlow()

    init {
        sock.connect()
        sock.incomingMessages
            .filter { it.jid == ui.value.jid }
            .onEach { msg ->
                append(msg)
                store.addMessage(msg.jid, msg)
            }
            .launchIn(viewModelScope)
    }

    fun load(jid: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, jid = jid) }
        repo.getMessageHistory(jid)
            .onSuccess { msgs ->
                _ui.update { it.copy(isLoading = false, messages = msgs) }
            }
            .onFailure { e ->
                _ui.update { s -> s.copy(isLoading = false, error = e.message) }
            }
    }

    fun sendText(text: String) {
        val jid = ui.value.jid ?: return
        val msg = Message(jid = jid, text = text, isOutgoing = true)

        append(msg)
        store.addMessage(jid, msg)

        viewModelScope.launch {
            repo.sendTextMessage(jid, text, msg.id)
                .onFailure { e ->
                    _ui.update { s -> s.copy(error = e.message) }
                }
        }
    }

    private fun append(m: Message) {
        _ui.update { it.copy(messages = it.messages + m) }
    }

    override fun onCleared() {
        sock.disconnect()
    }

    data class Ui(
        val isLoading: Boolean = false,
        val jid: String? = null,
        val messages: List<Message> = emptyList(),
        val error: String? = null
    )
}
