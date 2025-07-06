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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- DEFINICIÓN ÚNICA DE CLASES DE ESTADO ---
data class ConversationUiItem(
    val contact: Contact,
    val lastMessage: Message?
)

data class ConversationListState(
    val conversations: List<ConversationUiItem> = emptyList()
)

data class SearchState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList()
)

sealed class SearchResultItem {
    data class ContactItem(val contact: Contact) : SearchResultItem()
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}

data class MainScreenUiState(
    val conversationState: ConversationListState = ConversationListState(),
    val searchState: SearchState = SearchState(),
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val contactToDelete: Contact? = null,
    val isMenuExpanded: Boolean = false
)
// --- FIN DE LAS DEFINICIONES ---


class ConversationListViewModel(
    private val application: Application,
    private val contactStorage: ContactStorage,
    private val messageStorage: MessageStorage
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _contactToDelete = MutableStateFlow<Contact?>(null)
    private val _isMenuExpanded = MutableStateFlow(false)

    val uiState: StateFlow<MainScreenUiState> = combine(
        contactStorage.contactsFlow,
        messageStorage.lastMessagesMapFlow,
        _isLoading,
        _isSearchActive,
        _searchQuery, // ARREGLO: Se quitó el .debounce(300) de aquí para que el texto se actualice al instante.
        _contactToDelete,
        _isMenuExpanded,
        messageStorage.allMessagesFlow
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val contacts = flows[0] as List<Contact>
        @Suppress("UNCHECKED_CAST")
        val lastMessagesMap = flows[1] as Map<String, Message>
        val isLoading = flows[2] as Boolean
        val isSearchActive = flows[3] as Boolean
        val query = flows[4] as String
        @Suppress("UNCHECKED_CAST")
        val contactToDelete = flows[5] as Contact?
        val isMenuExpanded = flows[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val allMessages = flows[7] as List<Message>

        val contactsMap = contacts.associateBy { it.id }
        val allJids = (contacts.map { it.id } + lastMessagesMap.keys).toSet()
        val conversationItems = allJids.mapNotNull { jid ->
            val contact = contactsMap[jid] ?: Contact(
                id = jid,
                name = jid.substringBefore('@'),
                phoneNumber = null,
                lastMessageTimestamp = lastMessagesMap[jid]?.timestamp,
                isGroup = jid.endsWith("@g.us")
            )
            val lastMessage = lastMessagesMap[jid]
            if (contactsMap[jid] == null && lastMessage == null) null
            else ConversationUiItem(contact, lastMessage)
        }.sortedByDescending { it.lastMessage?.timestamp ?: it.contact.lastMessageTimestamp ?: 0 }
        val conversationState = ConversationListState(conversationItems)

        // La lógica de búsqueda ahora se ejecuta en cada cambio de texto, pero en un hilo secundario gracias a flowOn.
        val searchResults = if (query.isBlank()) {
            emptyList()
        } else {
            val contactResults = contacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumber?.contains(query) == true
            }.map { SearchResultItem.ContactItem(it) }

            val messageResults = allMessages.filter { message ->
                message.text?.contains(query, ignoreCase = true) == true
            }.mapNotNull { message ->
                contactsMap[message.jid]?.let { contact ->
                    SearchResultItem.MessageItem(message, contact)
                }
            }
            (contactResults + messageResults).distinctBy { result ->
                when (result) {
                    is SearchResultItem.ContactItem -> result.contact.id
                    is SearchResultItem.MessageItem -> result.contact.id
                }
            }
        }
        val searchState = SearchState(query, searchResults)

        MainScreenUiState(
            conversationState = conversationState,
            searchState = searchState,
            isSearchActive = isSearchActive,
            isLoading = isLoading,
            contactToDelete = contactToDelete,
            isMenuExpanded = isMenuExpanded
        )
    }.flowOn(Dispatchers.Default)
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenUiState(isLoading = true)
    )

    init {
        refreshConversations()
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onSearchActiveChanged(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (!isActive) { _searchQuery.value = "" }
    }
    fun onShowDeleteDialog(contact: Contact?) { _contactToDelete.value = contact }
    fun onDeleteConversation(contact: Contact) = viewModelScope.launch {
        contactStorage.deleteContact(contact)
        onShowDeleteDialog(null)
    }
    fun onShowMenuChanged(isExpanded: Boolean) { _isMenuExpanded.value = isExpanded }

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
