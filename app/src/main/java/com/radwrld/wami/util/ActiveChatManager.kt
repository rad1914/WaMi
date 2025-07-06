// @path: app/src/main/java/com/radwrld/wami/util/ActiveChatManager.kt
package com.radwrld.wami.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatManager {

    private val _activeJid = MutableStateFlow<String?>(null)
    val activeJid = _activeJid.asStateFlow()

    
    fun setActiveChat(jid: String?) {
        _activeJid.value = jid
    }

    
    fun clearActiveChat() {
        _activeJid.value = null
    }
}
