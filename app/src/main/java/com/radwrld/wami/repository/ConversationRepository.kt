package com.radwrld.wami.repository

import android.app.Application

class ConversationRepository(application: Application) {
    fun getMessageHistory(jid: String, before: Long?) = runCatching { emptyList<com.radwrld.wami.network.Message>() }
    fun sendTextMessage(jid: String, text: String, id: String) = runCatching { SendMessageResponse(id) }
    fun sendMediaMessage(jid: String, id: String, file: java.io.File, progressCallback: ((Float) -> Unit)?) = runCatching { SendMessageResponse(id) }
    fun sendReaction(jid: String, messageId: String, emoji: String) = runCatching { Unit }
}

data class SendMessageResponse(val messageId: String)
