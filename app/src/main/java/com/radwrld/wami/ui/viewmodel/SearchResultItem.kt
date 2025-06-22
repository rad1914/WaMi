// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SearchResultItem.kt
package com.radwrld.wami.model

/**
 * Representa un único item en la lista de resultados de búsqueda.
 * Puede ser un contacto o un mensaje.
 */
sealed class SearchResultItem {
    /**
     * Un resultado de búsqueda que es un contacto.
     * @param contact El objeto Contact que coincidió con la búsqueda.
     */
    data class ContactItem(val contact: Contact) : SearchResultItem()

    /**
     * Un resultado de búsqueda que es un mensaje.
     * @param message El objeto Message que coincidió con la búsqueda.
     * @param contact El contacto al que pertenece el chat del mensaje.
     */
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}
