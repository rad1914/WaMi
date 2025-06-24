// @path: app/src/main/java/com/radwrld/wami/network/SyncService.kt
package com.radwrld.wami.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.radwrld.wami.MainActivity
import com.radwrld.wami.R

class SyncService : Service() {

    companion object {
        const val ACTION_START = "com.radwrld.wami.sync.ACTION_START"
        const val ACTION_STOP  = "com.radwrld.wami.sync.ACTION_STOP"
        private const val NOTIF_ID    = 1
        private const val CHANNEL_ID  = "SyncServiceChannel"
        private const val CHANNEL_NAME = "Wami Sync Service"
    }

    override fun onCreate() {
        super.onCreate()
        SyncManager.initialize(this)
        Logger.i("SyncService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                buildForegroundNotification()
                Logger.i("SyncService", "Starting—connecting socket")
                SyncManager.connect()
            }
            ACTION_STOP  -> {
                Logger.i("SyncService", "Stopping service")
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun buildForegroundNotification() {
        createChannelIfNeeded()
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wami Conectado")
            .setContentText("Escuchando mensajes en tiempo real")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notif)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("SyncService", "Destroyed—shutting down SyncManager")
        SyncManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
