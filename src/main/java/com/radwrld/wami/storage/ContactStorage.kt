// app/src/main/java/com/radwrld/wami/storage/ContactStorage.kt
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
        return gson.fromJson(jsonString, Array<Contact>::class.java).toList()
    }

    // --- APPLIED SUGGESTION: Added the missing addContact method ---
    fun addContact(newContact: Contact) {
        val currentContacts = getContacts().toMutableList()
        currentContacts.add(0, newContact) // Add to the top of the list
        saveContacts(currentContacts)
    }
}
