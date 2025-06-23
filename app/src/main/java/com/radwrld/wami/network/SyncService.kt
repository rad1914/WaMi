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
        const val ACTION_STOP = "com.radwrld.wami.sync.ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SyncServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        // El SyncManager se inicializa solo una vez, cuando el servicio se crea.
        SyncManager.initialize(this)
        Logger.i("SyncService", "SyncService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                Logger.i("SyncService", "Service starting, connecting socket...")
                SyncManager.connect()
            }
            ACTION_STOP -> {
                Logger.i("SyncService", "Service stopping...")
                stopSelf() // Esto llamará a onDestroy()
            }
        }
        // Si el sistema mata el servicio, queremos que se reinicie.
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wami Conectado")
            .setContentText("Escuchando nuevos mensajes en tiempo real.")
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener este ícono
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Wami Sync Service",
                NotificationManager.IMPORTANCE_LOW // Baja importancia para que no sea intrusiva
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("SyncService", "Service destroyed, shutting down SyncManager.")
        // Apaga el SyncManager para limpiar todo.
        SyncManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // No usamos binding, así que retornamos null.
        return null
    }
}
