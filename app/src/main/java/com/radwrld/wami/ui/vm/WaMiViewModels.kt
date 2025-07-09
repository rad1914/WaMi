// @path: app/src/main/java/com/radwrld/wami/ui/vm/WaMiViewModels.kt
package com.radwrld.wami.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ApiService
import com.radwrld.wami.data.Chat
import com.radwrld.wami.data.Message
import com.radwrld.wami.data.UserPreferencesRepository
import com.radwrld.wami.data.local.AppDatabase
import com.radwrld.wami.data.local.toChat
import com.radwrld.wami.data.local.toEntity
import com.radwrld.wami.data.local.toMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository(application)
    private val db = AppDatabase.getDatabase(application)

    private val _sessionId = MutableStateFlow<String?>(null)
    private val _qrCode = MutableStateFlow<String?>(null)
    private val _auth = MutableStateFlow(false)

    val sessionId = _sessionId.asStateFlow()
    val qrCode = _qrCode.asStateFlow()
    val isAuth = _auth.asStateFlow()

    fun loginWithId(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val (ok, _) = ApiService.getStatus(id)
        if (ok) {
            prefs.saveSessionId(id)
            _sessionId.value = id
            _auth.value = true
        }
    }

    fun start() = viewModelScope.launch(Dispatchers.IO) {
        val id = prefs.sessionIdFlow.firstOrNull()
        if (id != null && ApiService.getStatus(id).first) {
            _sessionId.value = id
            _auth.value = true
        } else {
            createNewSession()
        }
    }

    private fun createNewSession() = viewModelScope.launch(Dispatchers.IO) {
        val id = ApiService.createSession() ?: return@launch
        prefs.saveSessionId(id)
        _sessionId.value = id

        while (isActive && !_auth.value) {
            delay(3000)
            val (ok, qr) = ApiService.getStatus(id)
            _auth.value = ok
            _qrCode.value = qr
        }
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        sessionId.value?.let { id ->
            if (ApiService.logout(id)) {
                prefs.clearSessionId()
                _sessionId.value = null
                _auth.value = false
                _qrCode.value = null

                db.chatDao().clear()
                db.messageDao().clearAll()
            }
        }
    }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    fun load(sessionId: String) = viewModelScope.launch(Dispatchers.IO) {

        val local = db.chatDao().getAll().first().map { it.toChat() }
        if (local.isNotEmpty()) _chats.value = local

        val remote = ApiService.fetchChats(sessionId)
        db.chatDao().insertAll(remote.map { it.toEntity() })
        _chats.value = remote
    }
}

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val _msgs = MutableStateFlow<List<Message>>(emptyList())
    val msgs = _msgs.asStateFlow()

    fun load(sessionId: String, jid: String) = viewModelScope.launch(Dispatchers.IO) {

        val local = db.messageDao().getMessagesForJid(jid).first().map { it.toMessage() }
        if (local.isNotEmpty()) _msgs.value = local

        val remote = ApiService.fetchHistory(sessionId, jid)
        db.messageDao().insertAll(remote.map { it.toEntity(jid) })
        _msgs.value = remote
    }

    fun send(sessionId: String, jid: String, text: String) = viewModelScope.launch(Dispatchers.IO) {
        val confirmation = ApiService.sendText(sessionId, jid, text)
        confirmation?.let {
            val newMessage = Message(
                id = it.messageId,
                fromMe = true,
                text = text,
                timestamp = it.timestamp ?: System.currentTimeMillis(),
                reactions = emptyMap()
            )

            _msgs.value = listOf(newMessage) + _msgs.value

            db.messageDao().insertAll(listOf(newMessage.toEntity(jid)))
        }
    }
}
