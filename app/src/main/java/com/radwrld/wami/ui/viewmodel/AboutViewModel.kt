// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/AboutViewModel.kt
package com.radwrld.wami.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AboutUiState(
    val contactName: String = "",
    val avatarUrl: String? = null,
    val lastSeen: String? = null,
    val info: String = "",
    val phoneNumber: String? = null,
    val localTime: String = "--:--",
    val mediaCount: Int = 0,
    val commonGroupsCount: Int = 0,
    val isGroup: Boolean = false,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AboutViewModel(
    private val jid: String,
    private val repository: WhatsAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState

    fun toggleBlockContact() {  }
    fun reportContact() {  }

    companion object {
        fun provideFactory(
            jid: String,
            repository: WhatsAppRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AboutViewModel(jid, repository) as T
            }
        }
    }
}
