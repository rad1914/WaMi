// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SocialViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.network.SyncManager
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.ContactStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SocialUiState(
    val statuses: List<StatusItem> = emptyList(),
    val isLoading: Boolean = false
)

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    private val whatsAppRepository = WhatsAppRepository(application)
    private val contactStorage = ContactStorage(application)
    private val _uiState = MutableStateFlow(SocialUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        fetchStatuses()
        viewModelScope.launch {
            SyncManager.newStatusEvent.collect { newStatuses ->
                _uiState.update { currentState ->
                    val combined = (newStatuses + currentState.statuses).distinctBy { it.id }
                    currentState.copy(statuses = combined)
                }
            }
        }
    }

    fun fetchStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val contacts = contactStorage.contactsFlow.first()
            whatsAppRepository.getStatuses(contacts)
                .onSuccess { statuses -> _uiState.update { it.copy(statuses = statuses, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
