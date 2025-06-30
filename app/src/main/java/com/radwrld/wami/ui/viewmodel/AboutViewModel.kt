// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/AboutViewModel.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.GroupStorage
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AboutUiState(
    val contactName: String = "",
    val avatarUrl: String? = null,
    val lastSeen: String? = null,
    val info: String = "",
    val phoneNumber: String? = null,
    val mediaCount: Int = 0,
    val commonGroupsCount: Int = 0,
    val isGroup: Boolean = false,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val localTime: String? = null, // CORRECCIÓN: Se añade el campo
    val isLoading: Boolean = true,
    val error: String? = null
)

class AboutViewModel(
    application: Application,
    private val jid: String
) : AndroidViewModel(application) {

    private val contactStorage = ContactStorage(application)
    private val groupStorage = GroupStorage(application)
    private val messageStorage = MessageStorage(application)

    val uiState: StateFlow<AboutUiState> = combine(
        contactStorage.contactsFlow,
        groupStorage.groupInfoFlow
    ) { contacts, groupsInfo ->
        val contact = contacts.find { it.id == jid }

        if (contact == null) {
            return@combine AboutUiState(isLoading = false, error = "Contacto no encontrado")
        }

        val commonGroupsCount = if (!contact.isGroup) {
            val userGroups = contacts.filter { it.isGroup }
            userGroups.count { groupContact ->
                groupsInfo[groupContact.id]?.participants?.any { participant -> participant.id == jid } ?: false
            }
        } else {
            0
        }
        
        // CORRECCIÓN: Se añade lógica para el campo localTime
        val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        AboutUiState(
            isLoading = false,
            contactName = contact.name,
            avatarUrl = contact.avatarUrl,
            isGroup = contact.isGroup,
            commonGroupsCount = commonGroupsCount,
            localTime = currentTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AboutUiState(isLoading = true)
    )

    fun toggleBlockContact() { /* Lógica futura aquí */ }
    fun reportContact() { /* Lógica futura aquí */ }
}