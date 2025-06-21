// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SocketManager
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConversationListState(
    val conversations: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)
    private val socketManager: SocketManager = ApiClient.getSocketManager(application)
    private val messageStorage = MessageStorage(application) // ++ NUEVA dependencia

    private val _state = MutableStateFlow(ConversationListState())
    val state = _state.asStateFlow()

    init {
        loadFromCache()
        load() // Network refresh
        listenForIncomingMessages()
    }

    private fun loadFromCache() {
        val cachedConversations = repository.getCachedConversations()
        _state.update {
            it.copy(conversations = cachedConversations.sortedByDescending { c -> c.lastMessageTimestamp })
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.refreshAndGetConversations()
                .onSuccess { conversations ->
                    // ++ LÓGICA ACTUALIZADA: Obtener el último mensaje localmente
                    val updatedConversations = conversations.map { contact ->
                        val lastMessage = messageStorage.getLastMessage(contact.id)
                        if (lastMessage != null) {
                            contact.copy(
                                lastMessage = lastMessage.text ?: "Media",
                                lastMessageTimestamp = lastMessage.timestamp
                            )
                        } else {
                            contact // Mantener el del servidor si no hay mensajes locales
                        }
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            conversations = updatedConversations.sortedByDescending { c -> c.lastMessageTimestamp }
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
            _state.update { currentState ->
                val currentConversations = currentState.conversations.toMutableList()
                val conversationIndex = currentConversations.indexOfFirst { it.id == message.jid }
                val updatedConversation: Contact

                if (conversationIndex != -1) {
                    val existing = currentConversations.removeAt(conversationIndex)
                    updatedConversation = existing.copy(
                        lastMessage = message.text ?: "Media",
                        lastMessageTimestamp = message.timestamp,
                        unreadCount = if (!message.isOutgoing) existing.unreadCount + 1 else existing.unreadCount
                    )
                } else {
                    updatedConversation = Contact(
                        id = message.jid,
                        name = message.senderName ?: message.jid.split('@').firstOrNull() ?: "Unknown",
                        isGroup = message.jid.endsWith("@g.us"),
                        phoneNumber = if (!message.jid.endsWith("@g.us")) message.jid.split('@').firstOrNull() else null,
                        lastMessage = message.text ?: "Media",
                        lastMessageTimestamp = message.timestamp,
                        unreadCount = 1,
                        avatarUrl = "${repository.getBaseUrl()}/avatar/${message.jid}"
                    )
                }
                
                val finalList = (listOf(updatedConversation) + currentConversations)
                    .distinctBy { it.id }
                    .sortedByDescending { it.lastMessageTimestamp }

                repository.updateAndSaveConversations(finalList)
                
                currentState.copy(conversations = finalList)
            }
        }.launchIn(viewModelScope)
    }

    fun hide(jid: String) {
        val currentList = _state.value.conversations
        val updatedList = currentList.filterNot { it.id == jid }
        repository.updateAndSaveConversations(updatedList)
        _state.update { it.copy(conversations = updatedList) }
    }
}
