// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SearchResultItem.kt
package com.radwrld.wami.model

sealed class SearchResultItem {
    
    data class ContactItem(val contact: Contact) : SearchResultItem()

    
    data class MessageItem(val message: Message, val contact: Contact) : SearchResultItem()
}
