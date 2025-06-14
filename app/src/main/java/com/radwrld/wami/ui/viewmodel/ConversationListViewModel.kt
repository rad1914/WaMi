package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.HiddenConversationStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)
    private val hiddenConversationStorage = HiddenConversationStorage(application)

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getConversations()
            result.onSuccess { allConversations ->
                val hiddenJids = hiddenConversationStorage.getHiddenJids()
                val visibleConversations = allConversations.filterNot { it.jid in hiddenJids }
                _uiState.update {
                    it.copy(isLoading = false, conversations = visibleConversations)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message)
                }
            }
        }
    }

    fun hideConversation(jid: String) {
        hiddenConversationStorage.hideConversation(jid)
        _uiState.update { state ->
            val updatedList = state.conversations.filterNot { it.jid == jid }
            state.copy(conversations = updatedList)
        }
    }
    
    data class ConversationListUiState(
        val isLoading: Boolean = false,
        val conversations: List<Conversation> = emptyList(),
        val error: String? = null
    )
}
