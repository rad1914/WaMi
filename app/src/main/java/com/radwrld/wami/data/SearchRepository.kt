package com.radwrld.wami.repository

import android.content.Context
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.ui.viewmodel.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(context: Context) {

    private val contactStorage = ContactStorage(context)
    private val messageStorage = MessageStorage(context)

    
    suspend fun search(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val contactResults = searchContacts(query)
        val messageResults = searchMessages(query)

        messageResults + contactResults
    }

    private fun searchContacts(query: String): List<SearchResultItem.ContactItem> {
        return contactStorage.getContacts()
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { SearchResultItem.ContactItem(it) }
    }

    private fun searchMessages(query: String): List<SearchResultItem.MessageItem> {
        val allContacts = contactStorage.getContacts()
        val messageResults = mutableListOf<SearchResultItem.MessageItem>()

        for (contact in allContacts) {
            val matchingMessages = messageStorage.getMessages(contact.id)
                .filter { it.text?.contains(query, ignoreCase = true) == true }

            matchingMessages.forEach { message ->
                messageResults.add(SearchResultItem.MessageItem(message, contact))
            }
        }

        return messageResults.sortedByDescending { it.message.timestamp }
    }
}
