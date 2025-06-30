// @path: app/src/main/java/com/radwrld/wami/storage/ContactStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.radwrld.wami.network.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ContactStorage(context: Context) {
    private val prefs = context.getSharedPreferences("wami_contacts_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _contactsFlow = MutableStateFlow<List<Contact>>(emptyList())
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow.asStateFlow()

    init {
        _contactsFlow.value = loadContactsFromPrefs()
    }

    fun getContact(jid: String): Flow<Contact?> {
        return contactsFlow.map { list -> list.find { it.id == jid } }
    }

    fun upsertContacts(newContacts: List<Contact>) {
        val existingContacts = _contactsFlow.value.associateBy { it.id }.toMutableMap()
        newContacts.forEach { newContact ->
            existingContacts[newContact.id] = newContact
        }
        saveContactsToPrefs(existingContacts.values.toList())
    }

    fun upsertContact(contact: Contact) {
        upsertContacts(listOf(contact))
    }

    fun deleteContact(contact: Contact) {
        val updatedList = _contactsFlow.value.filterNot { it.id == contact.id }
        saveContactsToPrefs(updatedList)
    }

    private fun saveContactsToPrefs(contacts: List<Contact>) {
        val sortedList = contacts.sortedByDescending { it.lastMessageTimestamp ?: 0 }
        prefs.edit().putString("contacts", gson.toJson(sortedList)).apply()
        _contactsFlow.value = sortedList
    }

    private fun loadContactsFromPrefs(): List<Contact> = try {
        prefs.getString("contacts", null)?.let { json ->
            gson.fromJson(json, Array<Contact>::class.java).toList()
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
