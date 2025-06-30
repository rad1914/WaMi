// @path: app/src/main/java/com/radwrld/wami/sync/SyncWorker.kt
package com.radwrld.wami.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.GroupStorage

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val contactStorage = ContactStorage(applicationContext)
    private val groupStorage = GroupStorage(applicationContext)
    private val whatsAppRepository = WhatsAppRepository(applicationContext)

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Sincronización periódica iniciada.")
        return try {
            whatsAppRepository.fetchConversations().onSuccess { contacts ->
                contactStorage.upsertContacts(contacts)
                Log.d("SyncWorker", "${contacts.size} contactos sincronizados.")

                val groupJidsToSync = contacts.filter { it.isGroup }.map { it.id }
                Log.d("SyncWorker", "Sincronizando detalles de ${groupJidsToSync.size} grupos...")
                groupJidsToSync.forEach { jid ->
                    whatsAppRepository.getGroupInfo(jid).onSuccess { groupInfo ->
                        groupStorage.saveGroupInfo(groupInfo)
                    }.onFailure { e ->
                         Log.e("SyncWorker", "Falló la sincronización del grupo $jid", e)
                    }
                }
            }.onFailure {
                throw it
            }

            Log.d("SyncWorker", "Sincronización periódica finalizada con éxito.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "La sincronización periódica falló", e)
            Result.failure()
        }
    }
}