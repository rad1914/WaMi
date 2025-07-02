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

data class ConversationUiItem(
    val contact: Contact,
    val lastMessage: Message?
)

data class ConversationListState(
    val conversations: List<ConversationUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchState(
    val query: String = "",
    val results: List<Contact> = emptyList(),
    val isLoading: Boolean = false
)

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

    // LÓGICA DE BÚSQUEDA REACTIVA
    val searchState: StateFlow<SearchState> = _searchQuery
        .debounce(300) // Evita ejecutar la búsqueda en cada letra tecleada
        .combine(contactStorage.contactsFlow) { query, contacts ->
            if (query.isBlank()) {
                SearchState(query = query, results = emptyList())
            } else {
                val results = contacts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumber?.contains(query) == true
                }
                SearchState(query = query, results = results)
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

    fun hide(contact: Contact) = viewModelScope.launch {
        contactStorage.deleteContact(contact)
    }
}

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