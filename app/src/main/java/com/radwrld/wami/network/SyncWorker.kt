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
                    groupsToSync.forEach { group: Contact -> 
                        launch { 
                            whatsAppRepository.getGroupInfo(group.id).onSuccess { groupInfo: GroupInfo -> 
                                groupStorage.saveGroupInfo(groupInfo) 
                            }.onFailure { e: Throwable -> 
                                Log.e("SyncWorker", "Failed to sync group ${group.id}", e) 
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
