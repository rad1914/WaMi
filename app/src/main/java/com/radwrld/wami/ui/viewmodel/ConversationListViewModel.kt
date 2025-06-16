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
        // Load initial data and start listening for real-time updates.
        load()
        listenForIncomingMessages()
    }

    /**
     * Fetches the full list of conversations from the repository.
     * This is used for the initial load and for refreshing the list
     * when a new conversation is detected.
     */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getConversations()
                .onSuccess { conversations ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            // Ensure the list is always sorted by the latest message.
                            conversations = conversations.sortedByDescending { c -> c.lastMessageTimestamp }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    /**
     * Listens to the SocketManager for incoming messages and updates the UI state accordingly.
     */
    private fun listenForIncomingMessages() {
        viewModelScope.launch {
            socketManager.incomingMessages.collect { message ->
                // Check if a conversation for this message's JID already exists in our current state.
                val conversationIndex = _state.value.conversations.indexOfFirst { it.id == message.jid }

                if (conversationIndex != -1) {
                    // --- Conversation Exists: Perform an efficient in-memory update ---
                    _state.update { currentState ->
                        val conversations = currentState.conversations.toMutableList()
                        // Remove the old instance of the conversation.
                        val existingConversation = conversations.removeAt(conversationIndex)

                        // Create the updated conversation with new details.
                        val updatedConversation = existingConversation.copy(
                            lastMessage = message.text ?: "Media",
                            lastMessageTimestamp = message.timestamp,
                            // Increment unread count only for incoming messages.
                            unreadCount = if (!message.isOutgoing) existingConversation.unreadCount + 1 else existingConversation.unreadCount
                        )

                        // Add the updated conversation to the top of the list and update the state.
                        currentState.copy(
                            conversations = listOf(updatedConversation) + conversations
                        )
                    }
                } else {
                    // --- New Conversation: Trigger a full reload ---
                    // If this is a message from a brand new chat, the most reliable
                    // way to get all its details (name, avatar, etc.) is to
                    // reload the entire conversation list from the server.
                    load()
                }
            }
        }
    }
    
    /**
     * Hides a conversation locally from the list.
     * A full implementation might also call an API to archive the chat.
     */
    fun hide(jid: String) {
        _state.update { currentState ->
            currentState.copy(
                conversations = currentState.conversations.filterNot { it.id == jid }
            )
        }
    }
}
