// @path: app/src/main/java/com/radwrld/wami/storage/HiddenConversationStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the storage of JIDs for conversations that the user has hidden.
 * This allows the app to remember which conversations not to display.
 */
class HiddenConversationStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Adds a conversation JID to the set of hidden conversations.
     *
     * @param jid The JID of the conversation to hide.
     */
    fun hideConversation(jid: String) {
        val hiddenJids = getHiddenJids().toMutableSet()
        hiddenJids.add(jid)
        prefs.edit().putStringSet(KEY_HIDDEN_JIDS, hiddenJids).apply()
    }

    /**
     * Retrieves the set of all hidden conversation JIDs.
     *
     * @return A set of strings, where each string is a JID of a hidden conversation.
     */
    fun getHiddenJids(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_JIDS, emptySet()) ?: emptySet()
    }

    /**
     * Clears all stored hidden conversation JIDs, making all conversations visible again.
     */
    fun clearAll() {
        prefs.edit().remove(KEY_HIDDEN_JIDS).apply()
    }

    companion object {
        private const val PREFS_NAME = "hidden_conversation_prefs"
        private const val KEY_HIDDEN_JIDS = "hidden_jids"
    }
}
