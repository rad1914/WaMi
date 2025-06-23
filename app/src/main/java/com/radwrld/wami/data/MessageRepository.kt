// @path: app/src/main/java/com/radwrld/wami/data/MessageRepository.kt
package com.radwrld.wami.data

import android.content.Context
import com.radwrld.wami.model.Message
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MessageRepository(context: Context) {
    private val storage = MessageStorage(context.applicationContext)
    private val flows = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val mutex = Mutex()

    private suspend fun getOrCreateFlow(jid: String): MutableStateFlow<List<Message>> {
        mutex.withLock {
            return flows.getOrPut(jid) {
                MutableStateFlow(storage.getMessages(jid))
            }
        }
    }
    
    fun getMessages(jid: String): Flow<List<Message>> = flow {
        emit(getOrCreateFlow(jid).value)
        emitAll(getOrCreateFlow(jid))   
    }

    private suspend fun updateAndNotify(jid: String) {
        val newList = storage.getMessages(jid)
        getOrCreateFlow(jid).emit(newList)
    }

    suspend fun addMessage(jid: String, message: Message) = withContext(Dispatchers.IO) {
        storage.addMessage(jid, message)
        updateAndNotify(jid)
    }

    suspend fun appendMessages(jid: String, messages: List<Message>) = withContext(Dispatchers.IO) {
        storage.appendMessages(jid, messages)
        updateAndNotify(jid)
    }

    suspend fun updateMessageStatus(jid: String, id: String, status: String) = withContext(Dispatchers.IO) {
        storage.updateMessageStatus(jid, id, status)
        updateAndNotify(jid)
    }

    suspend fun updateMessageReactions(jid: String, id: String, reactions: Map<String, Int>) = withContext(Dispatchers.IO) {
        storage.updateMessageReactions(jid, id, reactions)
        updateAndNotify(jid)
    }

    suspend fun updateMessageLocalPath(jid: String, id: String, path: String) = withContext(Dispatchers.IO) {
        storage.updateMessageLocalPath(jid, id, path)
        updateAndNotify(jid)
    }

    suspend fun updateMessage(jid: String, tempId: String, newId: String, newStatus: String) = withContext(Dispatchers.IO) {
        storage.updateMessage(jid, tempId, newId, newStatus)
        updateAndNotify(jid)
    }

    suspend fun saveMessages(jid: String, messages: List<Message>) = withContext(Dispatchers.IO) {
        storage.saveMessages(jid, messages)
        updateAndNotify(jid)
    }
}
