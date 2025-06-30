package com.radwrld.wami.ui.viewmodel

import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message

sealed class SearchResultItem {
    data class ContactItem(val contact: Contact) : SearchResultItem()
    // ¡AQUÍ ESTÁ EL CAMBIO! Ahora también pide el contacto.
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}