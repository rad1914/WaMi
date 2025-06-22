// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ContactRepository
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.SearchResultItem
import com.radwrld.wami.repository.SearchRepository
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConversationListState(
    val conversations: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ++ NUEVO: Estado para la UI de búsqueda
data class SearchState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val whatsAppRepository = WhatsAppRepository(application)
    private val contactRepository = ContactRepository(application)
    // ++ NUEVO: Repositorio de búsqueda
    private val searchRepository = SearchRepository(application)

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    // ++ NUEVO: Flujo para el estado de búsqueda
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    private var searchJob: Job? = null


    // El estado de la UI se construye combinando el Flow de contactos del repositorio
    // con los estados de carga y error.
    val conversationState: StateFlow<ConversationListState> = combine(
        contactRepository.contactsFlow,
        _isLoading,
        _error
    ) { conversations, isLoading, error ->
        ConversationListState(
            conversations = conversations,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationListState()
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            whatsAppRepository.refreshAndGetConversations()
                .onSuccess { conversationsFromServer ->
                    contactRepository.saveContacts(conversationsFromServer)
                }
                .onFailure { error ->
                    _error.value = error.message
                }
            _isLoading.value = false
        }
    }

    fun hide(jid: String) {
        viewModelScope.launch {
            val contactToHide = conversationState.value.conversations.find { it.id == jid }
            if (contactToHide != null) {
                contactRepository.deleteContact(contactToHide)
            }
        }
    }

    /**
     * ++ NUEVO: Se activa cuando el texto de búsqueda cambia.
     * Inicia una búsqueda con un retardo (debounce) para no buscar en cada letra.
     */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        _searchState.value = _searchState.value.copy(query = query)

        if (query.isBlank()) {
            _searchState.value = SearchState() // Resetea el estado de búsqueda
            return
        }

        searchJob = viewModelScope.launch {
            delay(300L) // Debounce para evitar búsquedas excesivas
            _searchState.value = _searchState.value.copy(isSearching = true)
            val results = searchRepository.search(query)
            _searchState.value = _searchState.value.copy(results = results)
        }
    }
}
