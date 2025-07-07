// SessionViewModel.kt
package com.radwrld.wami.ui.vm
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

class SessionViewModel: ViewModel() {
    private val _sessionId = MutableStateFlow<String?>(null)
    private val _qrCode    = MutableStateFlow<String?>(null)
    private val _auth      = MutableStateFlow(false)
    val sessionId = _sessionId.asStateFlow()
    val qrCode    = _qrCode.asStateFlow()
    val isAuth    = _auth.asStateFlow()

    fun start() {
        viewModelScope.launch {
            val id = ApiService.createSession() ?: return@launch
            _sessionId.value = id
            while (!_auth.value) {
                delay(3000)
                val (ok, qr) = ApiService.getStatus(id)
                _auth.value = ok
                _qrCode.value = qr
            }
        }
    }

    fun logout() {
        sessionId.value?.let { ApiService.logout(it) }
        _sessionId.value = null
        _auth.value = false
        _qrCode.value = null
    }
}

// ChatViewModel.kt
package com.radwrld.wami.ui.vm
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import com.radwrld.wami.data.Chat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel: ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    fun load(sessionId: String) = viewModelScope.launch {
        _chats.value = ApiService.fetchChats(sessionId)
    }
}

// MessageViewModel.kt
package com.radwrld.wami.ui.vm
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import com.radwrld.wami.data.Message
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MessageViewModel: ViewModel() {
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(sessionId: String, jid: String) = viewModelScope.launch {
        _msgs.value = ApiService.fetchHistory(sessionId, jid)
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch {
        ApiService.sendText(sessionId, jid, text)
        load(sessionId, jid)
    }
}
