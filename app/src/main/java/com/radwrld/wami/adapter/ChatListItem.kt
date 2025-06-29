// @path: app/src/main/java/com/radwrld/wami/adapter/ChatListItem.kt
package com.radwrld.wami.adapter

import com.radwrld.wami.network.Message

sealed interface ChatListItem {

    val key: String

    data class MessageItem(val message: Message) : ChatListItem {
        override val key: String = message.id
    }

    data class DividerItem(val timestamp: Long) : ChatListItem {
        override val key: String = "divider-$timestamp"
    }

    object WarningItem : ChatListItem {
        override val key: String = "warning-item"
    }
}
