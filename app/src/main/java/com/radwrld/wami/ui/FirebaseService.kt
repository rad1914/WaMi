// @path: app/src/main/java/com/radwrld/wami/adapter/FirebaseService.kt
package com.radwrld.wami.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.radwrld.wami.util.NotificationUtils
import org.json.JSONObject

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Here you would send the token to your server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if the message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            try {
                val data = remoteMessage.data
                val jid = data["jid"]
                val messageText = data["text"]
                val contactName = data["name"]
                val messageId = data["id"]

                if (jid != null && messageText != null && contactName != null && messageId != null) {
                    NotificationUtils.showNotification(
                        context = this,
                        jid = jid,
                        contactName = contactName,
                        message = messageText,
                        messageId = messageId
                    )
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error processing data message", e)
            }
        }
    }
}
