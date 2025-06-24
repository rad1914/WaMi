// @path: app/src/main/java/com/radwrld/wami/storage/ContactStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.radwrld.wami.network.Contact

class ContactStorage(context: Context) {
    private val prefs = context.getSharedPreferences("contacts_pref", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun loadList(): MutableList<Contact> = try {
        gson.fromJson(prefs.getString("contacts", "[]"), Array<Contact>::class.java)
            .toMutableList()
    } catch (e: Exception) {
        mutableListOf()
    }

    private fun saveList(list: List<Contact>) {
        prefs.edit().putString("contacts", gson.toJson(list)).apply()
    }

    fun getContacts(): List<Contact> = loadList()

    fun saveContacts(list: List<Contact>) = saveList(list)

    fun addContact(contact: Contact) {
        val list = loadList()
        if (list.none { it.id == contact.id }) {
            list.add(0, contact)
            saveList(list)
        }
    }

    fun deleteContact(contact: Contact) {
        val updated = loadList().filterNot { it.id == contact.id }
        saveList(updated)
    }
}
