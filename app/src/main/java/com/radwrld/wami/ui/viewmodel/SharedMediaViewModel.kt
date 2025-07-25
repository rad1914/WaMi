// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SharedMediaViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaItem(val uri: String, val type: String)

data class SharedMediaUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SharedMediaViewModel(application: Application, private val jid: String) : AndroidViewModel(application) {
    private val storage = MessageStorage(application)
    private val _uiState = MutableStateFlow(SharedMediaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val mediaMessages = storage.getMessages(jid).filter { it.hasMedia() }
                val items = mediaMessages.mapNotNull { msg ->
                    msg.mediaUrl?.let { uri -> MediaItem(uri, msg.mimetype ?: "") }
                }
                _uiState.update { it.copy(isLoading = false, mediaItems = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar multimedia") }
            }
        }
    }
}
