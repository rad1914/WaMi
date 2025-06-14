// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.HiddenConversationStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConversationListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo           = WhatsAppRepository(app)
    private val hiddenStorage  = HiddenConversationStorage(app)

    private val _state = MutableStateFlow(UiState())
    val state        = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        repo.getConversations()
            .onSuccess { all ->
                val hidden = hiddenStorage.getHiddenJids()
                _state.update {
                    it.copy(
                        isLoading      = false,
                        conversations  = all.filterNot { convo -> convo.jid in hidden }
                    )
                }
            }
            .onFailure { e ->
                _state.update { s ->
                    s.copy(isLoading = false, error = e.message)
                }
            }
    }

    fun hide(jid: String) {
        hiddenStorage.hideConversation(jid)
        _state.update { s ->
            s.copy(conversations = s.conversations.filterNot { it.jid == jid })
        }
    }

    data class UiState(
        val isLoading: Boolean              = false,
        val conversations: List<Conversation> = emptyList(),
        val error: String?                  = null
    )
}
