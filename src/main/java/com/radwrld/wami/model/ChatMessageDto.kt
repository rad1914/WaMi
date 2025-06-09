package com.radwrld.wami.model

data class ChatMessageDto(
    val text: String,
    val status: String,
    val jid: String,
    val isOutgoing: Int,
    val timestamp: Long
)
