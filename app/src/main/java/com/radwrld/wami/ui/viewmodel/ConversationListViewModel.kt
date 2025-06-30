// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val contactStorage = ContactStorage(application)
    private val messageStorage = MessageStorage(application)

    private var searchJob: Job? = null

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

    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    fun hide(contact: Contact) = viewModelScope.launch {

        contactStorage.deleteContact(contact)
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }

        _searchState.update { it.copy(isLoading = true) }
        searchJob = viewModelScope.launch {
            delay(300)

            val allContacts = contactStorage.contactsFlow.first()
            val results = allContacts.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.phoneNumber?.contains(query) == true
            }
            _searchState.update { it.copy(isLoading = false, results = results) }
        }
    }
}
