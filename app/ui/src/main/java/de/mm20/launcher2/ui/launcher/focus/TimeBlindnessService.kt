package de.mm20.launcher2.ui.launcher.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimeBlindnessService : Service() {

    private val searchUiSettings: SearchUiSettings by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Single polling job. We cancel/replace it on every start so a null-intent
    // (START restart) or a duplicate start cannot stack multiple pollers.
    private var pollingJob: Job? = null

    // Collects the relevant settings ONCE into [config] so the poller never has
    // to read DataStore per iteration. Launched in onCreate, cancelled in onDestroy.
    private var configJob: Job? = null

    // In-memory snapshot of the feature config, published by [configJob]. Null until
    // the first settings emission. A StateFlow so the poller can suspend on the first
    // value (no second DataStore subscription needed) and read `.value` each tick.
    private val config = MutableStateFlow<Config?>(null)

    private data class Config(
        val enabled: Boolean,
        val intervalMinutes: Int,
        val distractingKeys: Set<String>,
    )

    // Whether the screen is currently off. While true the poller does NOT wake
    // up at all: it suspends on `screenOff.first { !it }` with no timer until
    // USER_PRESENT flips it back to false.
    private val screenOff = MutableStateFlow(false)

    // Runtime-registered receiver for SCREEN_OFF / USER_PRESENT. These actions
    // are NOT deliverable to manifest-declared receivers on modern Android, so
    // they must be registered at runtime while the service is alive.
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> screenOff.value = true
                Intent.ACTION_USER_PRESENT -> screenOff.value = false
            }
        }
    }
    private var screenStateReceiverRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenStateReceiver()
        // Seed the initial screen state. SCREEN_OFF / USER_PRESENT are edge-triggered
        // and NOT sticky, so when the service starts while the screen is already off
        // (most importantly right after BOOT_COMPLETED, but also any background process
        // spawn), no broadcast would ever arrive and the poller would otherwise busy-poll
        // every minute with the screen off — exactly the wakeups this design removes.
        // Read after registering the receiver so a transition during startup is not lost.
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        screenOff.value = !powerManager.isInteractive
        startConfigCollector()
    }

    // Collect the three relevant settings ONCE into the in-memory [config]. The
    // poller reads this snapshot instead of calling .first() each iteration. If
    // the feature is disabled we stop the service promptly.
    private fun startConfigCollector() {
        configJob?.cancel()
        configJob = scope.launch {
            combine(
                searchUiSettings.focusTimeBlindnessRemindersEnabled,
                searchUiSettings.focusTimeBlindnessIntervalMinutes,
                searchUiSettings.focusDistractingAppKeys,
            ) { enabled, intervalMinutes, distractingKeys ->
                Config(
                    enabled = enabled,
                    intervalMinutes = intervalMinutes.coerceAtLeast(1),
                    distractingKeys = distractingKeys,
                )
            }.collect { newConfig ->
                config.value = newConfig
                if (!newConfig.enabled) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // We must always call startForeground promptly after being started as a
        // foreground service (including null-intent restarts), otherwise the
        // system kills us with an ANR/exception. Guard the typed overload for
        // API 34+ which requires the FOREGROUND_SERVICE_SPECIAL_USE permission.
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Cancel any previously running poller before starting a new one so that
        // duplicate starts and null-intent restarts cannot stack pollers.
        pollingJob?.cancel()
        pollingJob = scope.launch {
            // Bail out immediately (after the required startForeground above) if
            // the feature is disabled. This covers null-intent restarts too. The
            // config collector started in onCreate populates [config]; suspend on
            // the first non-null emission so we have a value to read.
            if (!config.filterNotNull().first().enabled) {
                stopSelf()
                return@launch
            }

            // Outer loop: while the screen is off this suspends on the StateFlow
            // with NO timer until USER_PRESENT flips it back, so there are ZERO
            // periodic wakeups while the screen is off.
            while (true) {
                screenOff.first { !it }

                var unbrokenMinutes = 0

                // Inner active loop: runs the usage check once a minute while the
                // screen is on. Breaks back out to suspend as soon as the screen
                // turns off again.
                while (!screenOff.value) {
                    delay(60_000L) // check every minute

                    if (screenOff.value) break

                    val currentConfig = config.value ?: continue
                    if (!currentConfig.enabled) {
                        stopSelf()
                        return@launch
                    }

                    val intervalMinutes = currentConfig.intervalMinutes
                    val distractingKeys = currentConfig.distractingKeys

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
        }

        // START_STICKY: if the system kills us under memory pressure we want the
        // service to come back and resume reminders. The restart is safe because
        // onStartCommand cancels any prior poller, always calls startForeground,
        // and re-checks the enabled flag (stopping itself with START_NOT_STICKY
        // semantics via stopSelf if the user disabled the feature meanwhile).
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
        pollingJob?.cancel()
        pollingJob = null
        configJob?.cancel()
        configJob = null
        unregisterScreenStateReceiver()
        scope.cancel()
    }

    private fun registerScreenStateReceiver() {
        if (screenStateReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        // SCREEN_OFF / USER_PRESENT are system protected broadcasts; NOT_EXPORTED
        // is appropriate (and required on API 34+ for non-exported receivers).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        screenStateReceiverRegistered = true
    }

    private fun unregisterScreenStateReceiver() {
        if (!screenStateReceiverRegistered) return
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered; ignore.
        }
        screenStateReceiverRegistered = false
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
