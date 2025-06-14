// @path: app/src/main/java/com/radwrld/wami/storage/ContactStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.radwrld.wami.model.Contact

class ContactStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("contacts_pref", Context.MODE_PRIVATE)

    private val gson = Gson()

    fun saveContacts(contacts: List<Contact>) {
        val jsonString = gson.toJson(contacts)
        sharedPreferences.edit().putString("contacts", jsonString).apply()
    }

    fun getContacts(): List<Contact> {
        val jsonString = sharedPreferences.getString("contacts", "[]")
        // Handle potential parsing errors
        return try {
            gson.fromJson(jsonString, Array<Contact>::class.java).toList()
        } catch (e: Exception) {
            emptyList() // Return an empty list if the stored data is corrupt
        }
    }

    fun addContact(newContact: Contact) {
        val currentContacts = getContacts().toMutableList()
        // Prevent adding duplicates
        if (currentContacts.none { it.id == newContact.id }) {
            currentContacts.add(0, newContact)
            saveContacts(currentContacts)
        }
    }

    fun deleteContact(contactToDelete: Contact) {
        val currentContacts = getContacts().toMutableList()
        // Use the unique ID (JID) for deletion
        val updatedContacts = currentContacts.filterNot { it.id == contactToDelete.id }
        saveContacts(updatedContacts)
    }
}
