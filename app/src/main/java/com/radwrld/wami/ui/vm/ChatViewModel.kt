// @path: app/src/main/java/com/radwrld/wami/ui/vm/ChatViewModel.kt
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
