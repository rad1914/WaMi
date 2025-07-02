// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Data & State Classes ---

data class ConversationUiItem(
    val contact: Contact,
    val lastMessage: Message?
)

data class ConversationListState(
    val conversations: List<ConversationUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SearchResultItem {
    data class ContactItem(val contact: Contact) : SearchResultItem()
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}

data class SearchState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false
)

// --- ViewModel ---

class ConversationListViewModel(
    private val contactStorage: ContactStorage,
    messageStorage: MessageStorage
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val conversationState: StateFlow<ConversationListState> = combine(
        contactStorage.contactsFlow,
        messageStorage.lastMessagesMapFlow
    ) { contacts, lastMessagesMap ->
        val uiItems = contacts
            .map { contact -> ConversationUiItem(contact, lastMessagesMap[contact.id]) }
            .sortedByDescending { it.lastMessage?.timestamp ?: it.contact.lastMessageTimestamp ?: 0 }

        ConversationListState(uiItems, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationListState(isLoading = true)
    )

    val searchState: StateFlow<SearchState> = _searchQuery
        .debounce(300)
        .combine(contactStorage.contactsFlow) { query, contacts -> // TODO: Also combine with a flow for all messages
            if (query.isBlank()) {
                SearchState(query = query, results = emptyList())
            } else {
                val contactResults = contacts.filter {
                    it.name.contains(query, ignoreCase = true) || it.phoneNumber?.contains(query) == true
                }.map { SearchResultItem.ContactItem(it) }
                
                // TODO: Implement message search logic here and combine results
                // val messageResults = allMessages.filter { ... }.map { SearchResultItem.MessageItem(it) }
                // val combinedResults = contactResults + messageResults

                SearchState(query = query, results = contactResults)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchState()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteConversation(contact: Contact) = viewModelScope.launch {
        contactStorage.deleteContact(contact)
    }
}

// --- ViewModel Factory ---

class ConversationListViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationListViewModel(
                contactStorage = ContactStorage(application),
                messageStorage = MessageStorage(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}