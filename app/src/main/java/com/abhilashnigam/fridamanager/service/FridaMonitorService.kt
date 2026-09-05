package com.abhilashnigam.fridamanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.abhilashnigam.fridamanager.frida.FridaManager
import com.abhilashnigam.fridamanager.widget.FridaWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "frida_monitor"
private const val NOTIFICATION_ID = 1
private const val POLL_INTERVAL_MS = 3000L

/**
 * Keeps a root shell "hot" and polls frida-server's running state on a short
 * interval so the home-screen widget reflects live status, per the "Live
 * status + toggle" requirement. Runs as a foreground service (low-priority,
 * persistent notification) rather than WorkManager, since WorkManager's
 * periodic floor (15 min) is far too coarse for this.
 */
class FridaMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch { pollLoop() }
    }

    private suspend fun pollLoop() {
        while (true) {
            val running = FridaManager.isRunning()
            FridaWidgetProvider.pushStatusUpdate(applicationContext, running)
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Frida Manager")
            .setContentText("Monitoring frida-server status")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Frida status monitor", NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)
    }
}
