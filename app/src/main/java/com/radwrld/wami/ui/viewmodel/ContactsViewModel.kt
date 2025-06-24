// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ContactsViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.data.ContactRepository
import com.radwrld.wami.model.Contact
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactRepository = ContactRepository(application)
    private val whatsAppRepository = WhatsAppRepository(application)

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.contactsFlow,
        _isLoading,
        _error
    ) { contacts, isLoading, error ->

        val (groups, individuals) = contacts.partition { it.isGroup }
        val sortedList = individuals.sortedBy { it.name.lowercase() } + groups.sortedBy { it.name.lowercase() }
        ContactsUiState(
            contacts = sortedList,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactsUiState()
    )

    fun syncContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            whatsAppRepository.refreshAndGetConversations()
                .onSuccess { conversationsFromServer ->

                    val existingContacts = uiState.value.contacts

                    val combinedMap = existingContacts.associateBy { it.id }.toMutableMap()
                    conversationsFromServer.forEach { serverContact ->
                        combinedMap[serverContact.id] = serverContact
                    }

                    contactRepository.saveContacts(combinedMap.values.toList())
                }
                .onFailure { error ->
                    _error.value = error.message
                }
            _isLoading.value = false
        }
    }
}
