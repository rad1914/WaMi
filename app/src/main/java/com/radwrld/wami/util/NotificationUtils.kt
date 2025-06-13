// @path: app/src/main/java/com/radwrld/wami/util/NotificationUtils.kt
package com.radwrld.wami.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.radwrld.wami.ChatActivity
import com.radwrld.wami.R

object NotificationUtils {

    private const val CHANNEL_ID = "wami_messages"
    private const val CHANNEL_NAME = "New Messages"

    fun showNotification(context: Context, jid: String, contactName: String, message: String, messageId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("EXTRA_JID", jid)
            putExtra("EXTRA_NAME", contactName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, jid.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contactName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use the messageId to generate a unique notification ID.
        // We use its hashCode() to get an Integer.
        notificationManager.notify(messageId.hashCode(), notification)
    }
}
