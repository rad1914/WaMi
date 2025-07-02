// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/ContactsViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.radwrld.wami.network.Contact
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.sync.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactStorage = ContactStorage(application)
    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contactStorage.contactsFlow
                .catch { throwable ->
                    _uiState.update { it.copy(error = throwable.message, isLoading = false) }
                }
                .collect { contacts ->
                    val (groups, individuals) = contacts.partition { it.isGroup }
                    val sortedList = individuals.sortedBy { it.name.lowercase() } + groups.sortedBy { it.name.lowercase() }
                    _uiState.update {
                        it.copy(contacts = sortedList, isLoading = false)
                    }
                }
        }
    }

    fun refreshContacts() {
        // Set loading state to true to show the refresh indicator in the UI
        _uiState.update { it.copy(isLoading = true) }
        // Enqueue the background sync worker
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        workManager.enqueue(workRequest)
    }
}