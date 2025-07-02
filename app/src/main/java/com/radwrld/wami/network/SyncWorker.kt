// @path: app/src/main/java/com/radwrld/wami/network/SyncWorker.kt
package com.radwrld.wami.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.radwrld.wami.data.WhatsAppRepository
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.GroupInfo
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.GroupStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val whatsAppRepository = WhatsAppRepository(applicationContext)
    private val contactStorage = ContactStorage(applicationContext)
    private val groupStorage = GroupStorage(applicationContext)

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Periodic sync started.")
        return try {
            whatsAppRepository.fetchConversations().onSuccess { contacts: List<Contact> ->

                contactStorage.upsertContacts(contacts)
                Log.d("SyncWorker", "${contacts.size} contacts synchronized.")

                val groupsToSync = contacts.filter { it.isGroup }
                Log.d("SyncWorker", "Syncing details for ${groupsToSync.size} groups...")

                coroutineScope {
                    val semaphore = Semaphore(5)

                    groupsToSync.forEach { group: Contact ->
                        launch {
                            semaphore.withPermit {
                                var successful = false
                                val maxRetries = 3
                                for (attempt in 1..maxRetries) {
                                    // Usamos try-catch en lugar de runCatching para evitar el 'break' experimental
                                    try {
                                        val groupInfo = whatsAppRepository.getGroupInfo(group.id).getOrThrow()
                                        groupStorage.saveGroupInfo(groupInfo)
                                        successful = true
                                        break // Ahora esto es válido y seguro
                                    } catch (e: Exception) {
                                        Log.e("SyncWorker", "Falló la sincronización del grupo ${group.id} (intento $attempt)", e)
                                        if (attempt < maxRetries) {
                                            kotlinx.coroutines.delay(1000L * attempt)
                                        }
                                    }
                                }
                                if (!successful) {
                                    Log.e("SyncWorker", "No se pudo sincronizar el grupo ${group.id} después de $maxRetries intentos.")
                                }
                            }
                        }
                    }
                }
            }.onFailure { e: Throwable ->
                throw e
            }

            Log.d("SyncWorker", "Periodic sync finished successfully.")

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Periodic sync failed", e)

            Result.failure()
        }
    }
}