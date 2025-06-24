// @path: app/src/main/java/com/radwrld/wami/data/ContactRepository.kt
package com.radwrld.wami.data

import android.content.Context
import com.radwrld.wami.network.Contact
import com.radwrld.wami.storage.ContactStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ContactRepository(context: Context) {
    private val storage = ContactStorage(context.applicationContext)

    private val _contactsFlow = MutableStateFlow<List<Contact>>(emptyList())
    val contactsFlow: Flow<List<Contact>> = _contactsFlow.asStateFlow()

    init {
        _contactsFlow.value = storage.getContacts()
    }

    private suspend fun refreshFlow() = withContext(Dispatchers.Main) {
        _contactsFlow.emit(storage.getContacts())
    }

    suspend fun addContact(contact: Contact) = withContext(Dispatchers.IO) {
        storage.addContact(contact)
        refreshFlow()
    }
    
    suspend fun saveContacts(contacts: List<Contact>) = withContext(Dispatchers.IO) {
        storage.saveContacts(contacts)
        refreshFlow()
    }

    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        storage.deleteContact(contact)
        refreshFlow()
    }
}
