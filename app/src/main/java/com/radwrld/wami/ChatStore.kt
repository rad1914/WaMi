// @path: app/src/main/java/com/radwrld/wami/ChatStore.kt
package com.radwrld.wami

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ChatStore {
    private val lock = Mutex()
    private var cached = emptyList<Message>()

    suspend fun update(new: List<Message>) = lock.withLock {
        if (new.isNotEmpty()) cached = new
    }

    suspend fun get(): List<Message> = lock.withLock { cached }
}
