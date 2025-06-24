package com.radwrld.wami.ui.viewmodel

import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message

sealed class SearchResultItem {
    data class ContactItem(val contact: Contact) : SearchResultItem()
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}
