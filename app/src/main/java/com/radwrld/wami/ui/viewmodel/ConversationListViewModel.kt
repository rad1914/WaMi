// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.storage.ServerConfigStorage
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

sealed class SearchResultItem {
    data class ContactItem(val contact: Contact) : SearchResultItem()
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}

data class SearchState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(), 
    val isLoading: Boolean = false
)

class ConversationListViewModel(
    private val application: Application,
    private val contactStorage: ContactStorage,
    private val messageStorage: MessageStorage

) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    val currentUserAvatarUrl: StateFlow<String?> = flow {
        val serverConfigStorage = ServerConfigStorage(application)
        val ownJid = serverConfigStorage.getOwnJid()
        val url = ownJid?.let { ApiClient.resolveAvatarUrl(application, it) }
        emit(url) 
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val conversationState: StateFlow<ConversationListState> = combine(
        contactStorage.contactsFlow,
        messageStorage.lastMessagesMapFlow,
        _isLoading
    ) { contacts, lastMessagesMap, isLoading ->
        val contactsMap = contacts.associateBy { it.id }
        val allJids = (contacts.map { it.id } + lastMessagesMap.keys).toSet() 

        val uiItems = allJids.mapNotNull { jid ->
            val contact = contactsMap[jid] ?: Contact(
                id = jid,
                name = jid.substringBefore('@'),
                phoneNumber = null,
                lastMessageTimestamp = lastMessagesMap[jid]?.timestamp, 
                isGroup = jid.endsWith("@g.us")
            )
            val lastMessage = lastMessagesMap[jid]

            if (contactsMap[jid] == null && lastMessage == null) {
                null
            } else { 
                ConversationUiItem(contact, lastMessage)
            }
        }.sortedByDescending { it.lastMessage?.timestamp ?: it.contact.lastMessageTimestamp ?: 0 }

        ConversationListState(uiItems, isLoading = isLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationListState(isLoading = true) 
    )

    val searchState: StateFlow<SearchState> = _searchQuery
        .debounce(300)
        .combine(contactStorage.contactsFlow) { query, contacts ->
            if (query.isBlank()) {
                SearchState(query = query, results = emptyList())
            } else {
                val contactResults = contacts.filter {
                    it.name.contains(query, ignoreCase = true) || 
                            it.phoneNumber?.contains(query) == true 
                }.map { SearchResultItem.ContactItem(it) }

                SearchState(query = query, results = contactResults)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchState() 
        )

    init {
        refreshConversations()
    }

    fun refreshConversations() = viewModelScope.launch {
        if (_isLoading.value) return@launch
        _isLoading.value = true
        Log.d("Sync", "Refreshing conversations...")
        try {
            val conversations = ApiClient.getInstance(application).getConversations()
            val contacts = conversations.map { convo ->
                Contact(
                    id = convo.jid,
                    name = convo.name ?: convo.jid.substringBefore('@'),
                    phoneNumber = if (convo.isGroup) null else convo.jid.substringBefore('@'),
                    lastMessageTimestamp = convo.lastMessageTimestamp,
                    unreadCount = convo.unreadCount ?: 0,
                    avatarUrl = ApiClient.resolveAvatarUrl(application, convo.jid),
                    isGroup = convo.isGroup
                )
            }
            contactStorage.upsertContacts(contacts)
            Log.d("Sync", "Successfully refreshed ${contacts.size} conversations.")
        } catch (e: Exception) {
            Log.e("Sync", "Refresh failed", e)
        } finally {
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteConversation(contact: Contact) = viewModelScope.launch { 
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
                application = application,
                contactStorage = ContactStorage.getInstance(application), 
                messageStorage = MessageStorage(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
