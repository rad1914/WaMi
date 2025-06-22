// @path: app/src/main/java/com/radwrld/wami/data/SearchRepository.kt
package com.radwrld.wami.repository

import android.content.Context
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.model.SearchResultItem
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar la lógica de búsqueda en toda la aplicación.
 * Busca tanto en contactos como en mensajes.
 */
class SearchRepository(context: Context) {
    // Usamos los 'Storage' directamente para una búsqueda simple y no reactiva.
    private val contactStorage = ContactStorage(context)
    private val messageStorage = MessageStorage(context)

    /**
     * Realiza una búsqueda en contactos y mensajes.
     * Esta operación puede ser intensiva, por lo que se ejecuta en el hilo de IO.
     *
     * @param query El texto a buscar.
     * @return Una lista de [SearchResultItem] que contiene contactos y mensajes coincidentes.
     */
    suspend fun search(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val contactResults = searchContacts(query)
        val messageResults = searchMessages(query)

        // Combina los resultados, los mensajes primero, y luego los contactos.
        // Los mensajes ya están ordenados por fecha descendente.
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

        // Ordenar todos los mensajes encontrados por fecha, los más recientes primero.
        return messageResults.sortedByDescending { it.message.timestamp }
    }
}
