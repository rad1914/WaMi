// @path: app/src/main/java/com/radwrld/wami/util/ActiveChatManager.kt
package com.radwrld.wami.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para rastrear el chat activo y suprimir notificaciones para ese chat.
 */
object ActiveChatManager {

    private val _activeJid = MutableStateFlow<String?>(null)
    val activeJid = _activeJid.asStateFlow()

    /**
     * Establece el JID del chat que está actualmente visible para el usuario.
     */
    fun setActiveChat(jid: String?) {
        _activeJid.value = jid
    }

    /**
     * Limpia el JID del chat activo, usualmente cuando el usuario sale de la pantalla de chat.
     */
    fun clearActiveChat() {
        _activeJid.value = null
    }
}
