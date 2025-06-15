// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ConversationListViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.model.Contact
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.HiddenConversationStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ConversationListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WhatsAppRepository(app)
    private val hiddenStorage = HiddenConversationStorage(app)
    private val contactStorage = ContactStorage(app)

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }

        // 1. Load contacts from local storage first for an instant, offline-capable UI.
        val cachedContacts = contactStorage.getContacts()
        _state.update {
            it.copy(
                isLoading = false,
                conversations = filterHidden(cachedContacts)
            )
        }

        // 2. Then, fetch fresh data from the network to sync.
        repo.getConversations()
            .onSuccess { freshContacts ->
                // 3. Save the fresh data to local storage for the next launch.
                contactStorage.saveContacts(freshContacts)
                
                // 4. Update the UI with the new, filtered data.
                _state.update {
                    it.copy(conversations = filterHidden(freshContacts))
                }
            }
            .onFailure { error ->
                // If the network fails, just log it. The user will see the cached data,
                // so we don't need to show a disruptive error message unless it's critical.
                Log.e("ConversationListViewModel", "Failed to sync conversations: ${error.message}")
                if (error is HttpException && error.code() == 401) {
                    _state.update { it.copy(error = "Session expired. Please log in again.") }
                }
            }
    }
    
    private fun filterHidden(contacts: List<Contact>): List<Contact> {
        val hiddenJids = hiddenStorage.getHiddenJids()
        return contacts.filterNot { it.id in hiddenJids }
    }

    fun hide(jid: String) {
        hiddenStorage.hideConversation(jid)
        _state.update { currentState ->
            currentState.copy(
                conversations = currentState.conversations.filterNot { it.id == jid }
            )
        }
    }

    data class UiState(
        val isLoading: Boolean = false,
        val conversations: List<Contact> = emptyList(),
        val error: String? = null
    )
}
