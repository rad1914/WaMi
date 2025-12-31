// @path: app/src/main/java/com/radwrld/wami/ChatStore.kt
package com.radwrld.wami

object ChatStore {
    @Volatile
    private var cached = emptyList<Message>()

    fun update(new: List<Message>) {
        if (new.isNotEmpty()) cached = new
    }

    fun get(): List<Message> = cached
}