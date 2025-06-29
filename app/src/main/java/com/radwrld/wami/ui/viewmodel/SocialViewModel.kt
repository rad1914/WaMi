// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SocialViewModel.kt
package com.radwrld.wami.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialViewModel(
    private val repository: WhatsAppRepository
) : ViewModel() {

    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses = _statuses.asStateFlow()

    fun fetchStatuses() {
        viewModelScope.launch {

        }
    }
}

class SocialViewModelFactory(
    private val repository: WhatsAppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
