// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SharedMediaViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaItem(
    val uri: String,
    val type: String
)

data class SharedMediaUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SharedMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)

    private val _uiState = MutableStateFlow(SharedMediaUiState())
    val uiState = _uiState.asStateFlow()

    fun loadMedia(jid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

             _uiState.update { it.copy(isLoading = false) }
        }
    }
}
