// @path: app/src/main/java/com/radwrld/wami/ui/FirebaseService.kt

package com.radwrld.wami.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.radwrld.wami.network.SyncManager
import com.radwrld.wami.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class FirebaseService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        Log.d("FCM", "Push notification received: $data")

        if (SyncManager.isConnected()) {
            Log.d("FCM", "Socket is connected, ignoring push notification.")
            return
        }

        when (data["type"]) {
            "message" -> handleMessageNotification(data)

            else -> Log.w("FCM", "Received unknown notification type: ${data["type"]}")
        }
    }

    private fun handleMessageNotification(data: Map<String, String>) {
        try {
            val jid = data["jid"]
            val messageText = data["text"]
            val contactName = data["name"]
            val messageId = data["id"]
            val messagePayload = data["payload"]

            if (messagePayload != null) {
                scope.launch {
                    SyncManager.initialize(applicationContext)
                    SyncManager.handleIncomingMessages(messagePayload, isSocketMessage = false)
                }
            }

            if (jid != null && messageText != null && contactName != null && messageId != null) {
                NotificationUtils.showNotification(
                    context = this,
                    jid = jid,
                    contactName = contactName,
                    message = messageText,
                    messageId = messageId
                )
            } else {
                Log.e("FCM", "Incomplete data for message notification: $data")
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error processing data message", e)
        }
    }
}
