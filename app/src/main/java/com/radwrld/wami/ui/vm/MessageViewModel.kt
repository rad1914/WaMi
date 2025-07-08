// @path: app/src/main/java/com/radwrld/wami/ui/vm/MessageViewModel.kt
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
