package de.mm20.launcher2.ui.launcher.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimeBlindnessService : Service() {

    private val searchUiSettings: SearchUiSettings by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        scope.launch {
            val isEnabled = searchUiSettings.focusTimeBlindnessRemindersEnabled.first()
            if (!isEnabled) {
                stopSelf()
                return@launch
            }

            var unbrokenMinutes = 0

            while (true) {
                delay(60_000L) // check every minute

                val intervalMinutes = searchUiSettings.focusTimeBlindnessIntervalMinutes.first().coerceAtLeast(1)
                val distractingKeys = searchUiSettings.focusDistractingAppKeys.first()

                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 60_000
                val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

                var currentApp: String? = null
                val event = android.app.usage.UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                        currentApp = event.packageName
                    } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) {
                        if (currentApp == event.packageName) {
                            currentApp = null
                        }
                    }
                }

                if (currentApp != null && distractingKeys.any { currentApp!!.startsWith(it) }) {
                    unbrokenMinutes++
                } else {
                    unbrokenMinutes = 0
                }

                if (unbrokenMinutes >= intervalMinutes) {
                    vibrate()
                    showTimeBlindnessAlert(intervalMinutes)
                    unbrokenMinutes = 0
                }
            }
        }

        return START_STICKY
    }

    private fun showTimeBlindnessAlert(minutes: Int) {
        launchOverlayIntent(minutes)
    }

    private fun launchOverlayIntent(minutes: Int) {
        val intent = Intent(this, de.mm20.launcher2.ui.launcher.focus.TimeBlindnessOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("minutes", minutes)
        }
        startActivity(intent)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time Blindness Reminders"
            val descriptionText = "Keeps time blindness reminders active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time Blindness Active")
            .setContentText("You will receive gentle reminders of time passing.")
            .setSmallIcon(R.drawable.timer_24px)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "time_blindness_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_START = "de.mm20.launcher2.action.START_TIME_BLINDNESS"
        const val ACTION_STOP = "de.mm20.launcher2.action.STOP_TIME_BLINDNESS"
    }
}
