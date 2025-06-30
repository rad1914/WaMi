// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ContactsViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.Contact
import com.radwrld.wami.storage.ContactStorage
import kotlinx.coroutines.flow.*

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactStorage = ContactStorage(application)
    
    val uiState: StateFlow<ContactsUiState> = contactStorage.contactsFlow
        .map { contacts ->
            val (groups, individuals) = contacts.partition { it.isGroup }
            val sortedList = individuals.sortedBy { it.name.lowercase() } + groups.sortedBy { it.name.lowercase() }
            ContactsUiState(contacts = sortedList)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactsUiState(isLoading = true)
        )

}
