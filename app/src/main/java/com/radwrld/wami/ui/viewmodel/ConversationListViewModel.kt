// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SocketManager
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class ConversationListState(
    val conversations: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)
    private val socketManager: SocketManager = ApiClient.getSocketManager(application)

    private val _state = MutableStateFlow(ConversationListState())
    val state = _state.asStateFlow()

    init {
        load()
        listenForIncomingMessages()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getConversations()
                .onSuccess { conversations ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            conversations = conversations.sortedByDescending { c -> c.lastMessageTimestamp }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun listenForIncomingMessages() {
        socketManager.incomingMessages.onEach { message ->
            val conversationIndex = _state.value.conversations.indexOfFirst { it.id == message.jid }

            _state.update { currentState ->
                val currentConversations = currentState.conversations.toMutableList()
                val updatedConversation: Contact

                if (conversationIndex != -1) {
                    // --- Conversation Exists: Perform an efficient in-memory update ---
                    val existing = currentConversations.removeAt(conversationIndex)
                    updatedConversation = existing.copy(
                        lastMessage = message.text ?: "Media",
                        lastMessageTimestamp = message.timestamp,
                        unreadCount = if (!message.isOutgoing) existing.unreadCount + 1 else existing.unreadCount
                    )
                } else {
                    // ++ IMPROVEMENT: New conversation is created in-memory instead of triggering a full network reload.
                    // This is vastly more efficient and provides an instant UI update.
                    val isGroup = message.jid.endsWith("@g.us")
                    updatedConversation = Contact(
                        id = message.jid,
                        name = message.senderName ?: message.jid.split('@').firstOrNull() ?: "Unknown",
                        phoneNumber = if (!isGroup) message.jid.split('@').firstOrNull() else null,
                        lastMessage = message.text ?: "Media",
                        lastMessageTimestamp = message.timestamp,
                        unreadCount = 1,
                        // Use the new repository method to build the avatar URL safely.
                        avatarUrl = "${repository.getBaseUrl()}/avatar/${URLEncoder.encode(message.jid, "UTF-8")}",
                        isGroup = isGroup
                    )
                }
                // Prepend the new or updated conversation to the top and update the state.
                currentState.copy(conversations = listOf(updatedConversation) + currentConversations)
            }
        }.launchIn(viewModelScope)
    }

    fun hide(jid: String) {
        _state.update { currentState ->
            currentState.copy(
                conversations = currentState.conversations.filterNot { it.id == jid }
            )
        }
    }
}
