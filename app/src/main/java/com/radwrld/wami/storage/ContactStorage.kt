// @path: app/src/main/java/com/radwrld/wami/storage/ContactStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.radwrld.wami.network.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max

class ContactStorage private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _contactsFlow = MutableStateFlow(loadContactsFromPrefs())
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow.asStateFlow()

    fun getContact(jid: String): Flow<Contact?> =
        contactsFlow.map { it.find { contact -> contact.id == jid } }

    fun resetUnreadCount(jid: String) {
        val updated = _contactsFlow.value.map {
            if (it.id == jid && it.unreadCount > 0) it.copy(unreadCount = 0) else it
        } 
        saveContactsToPrefs(updated)
    }

    fun upsertContacts(newContacts: List<Contact>) {
        val current = _contactsFlow.value.associateBy { it.id }.toMutableMap()

        for (new in newContacts) {
            val old = current[new.id]
            current[new.id] = if (old != null) { 
                old.copy(
                    name = if (old.name.isNotBlank() && old.name != new.id.substringBefore('@')) old.name else new.name,
                    lastMessageTimestamp = max(old.lastMessageTimestamp ?: 0L, new.lastMessageTimestamp ?: 0L),

                    unreadCount = old.unreadCount + new.unreadCount,
                    avatarUrl = new.avatarUrl ?: old.avatarUrl
                )
            } else new
        }

        saveContactsToPrefs(current.values.toList())
    }

    fun upsertContact(contact: Contact) {
        upsertContacts(listOf(contact)) 
    }

    fun deleteContact(contact: Contact) {
        val updated = _contactsFlow.value.filterNot { it.id == contact.id }
        saveContactsToPrefs(updated)
    }

    private fun saveContactsToPrefs(contacts: List<Contact>) {
        ioScope.launch {
            prefs.edit().putString(KEY, gson.toJson(contacts)).apply()
        }
        _contactsFlow.value = contacts
    }

    private fun loadContactsFromPrefs(): List<Contact> = try { 
        prefs.getString(KEY, null)?.let {
            gson.fromJson(it, Array<Contact>::class.java).toList()
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e("ContactStorage", "Failed to load contacts", e)
        emptyList()
    }

    fun clear() {
        ioScope.launch {
            prefs.edit().clear().apply()
        } 
        _contactsFlow.value = emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: ContactStorage? = null 

        private const val PREF_NAME = "wami_contacts_storage"
        private const val KEY = "contacts"

        fun getInstance(context: Context): ContactStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactStorage(context).also { INSTANCE = it }
            }
        }
    }
}
