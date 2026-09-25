package de.mm20.launcher2.ui.launcher.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.services.focus.FocusAppClassifier
import de.mm20.launcher2.services.focus.FocusAppType
import de.mm20.launcher2.services.focus.FocusForegroundController
import de.mm20.launcher2.services.focus.FocusPackageCandidate
import de.mm20.launcher2.services.focus.resolveUniquePersonalAppKey
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject

class TimeBlindnessService : Service() {

    private val searchUiSettings: SearchUiSettings by inject()
    private val appRepository: AppRepository by inject()
    private val focusAppClassifier: FocusAppClassifier by inject()
    private val foregroundController: FocusForegroundController by inject()
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
        val essentialKeys: Set<String>,
        val distractingKeys: Set<String>,
        val apps: List<Application>,
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
                searchUiSettings.focusEssentialAppKeys,
                searchUiSettings.focusDistractingAppKeys,
                appRepository.findMany(),
            ) { enabled, intervalMinutes, essentialKeys, distractingKeys, apps ->
                Config(
                    enabled = enabled,
                    intervalMinutes = intervalMinutes.coerceAtLeast(1),
                    essentialKeys = essentialKeys,
                    distractingKeys = distractingKeys,
                    apps = apps,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

            while (true) {
                screenOff.first { !it }
                if (!foregroundController.hasUsageAccess()) {
                    stopSelf()
                    return@launch
                }

                var checkedAtMillis = System.currentTimeMillis()
                var foregroundState = observeForegroundPackage(
                    state = ContinuousForegroundState(),
                    packageName = foregroundController.currentForegroundPackage(),
                    observedAtMillis = checkedAtMillis,
                )

                while (!screenOff.value) {
                    val nowMillis = System.currentTimeMillis()
                    val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                    val usageEvents = usageStatsManager.queryEvents(checkedAtMillis + 1L, nowMillis)
                    val event = android.app.usage.UsageEvents.Event()
                    while (usageEvents.hasNextEvent()) {
                        usageEvents.getNextEvent(event)
                        if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                            foregroundState = observeForegroundPackage(
                                state = foregroundState,
                                packageName = event.packageName,
                                observedAtMillis = event.timeStamp,
                            )
                        }
                    }
                    checkedAtMillis = nowMillis

                    val currentConfig = config.value ?: continue
                    if (!currentConfig.enabled) {
                        stopSelf()
                        return@launch
                    }
                    if (!foregroundController.hasUsageAccess()) {
                        stopSelf()
                        return@launch
                    }

                    val intervalMinutes = currentConfig.intervalMinutes
                    val foregroundAppKey = foregroundState.packageName?.let { packageName ->
                        val matchingApps = currentConfig.apps.filter {
                            it.componentName.packageName == packageName
                        }
                        resolveUniquePersonalAppKey(
                            matchingApps.map {
                                FocusPackageCandidate(
                                    appKey = it.key,
                                    isPersonalProfile = it.user == Process.myUserHandle(),
                                )
                            }
                        )
                    }
                    val isDistracting = foregroundAppKey != null && focusAppClassifier.classifyWith(
                        key = foregroundAppKey,
                        essentialKeys = currentConfig.essentialKeys,
                        distractingKeys = currentConfig.distractingKeys,
                    ) == FocusAppType.Distracting

                    val intervalMillis = intervalMinutes * 60_000L
                    var continuousMillis = if (isDistracting) {
                        checkedAtMillis - (foregroundState.sinceMillis ?: checkedAtMillis)
                    } else {
                        0L
                    }
                    if (continuousMillis >= intervalMillis) {
                        vibrate()
                        showTimeBlindnessAlert(intervalMinutes)
                        foregroundState = foregroundState.copy(sinceMillis = checkedAtMillis)
                        continuousMillis = 0L
                    }

                    val waitMillis = if (isDistracting) {
                        (intervalMillis - continuousMillis).coerceAtLeast(1_000L)
                    } else {
                        IDLE_CHECK_INTERVAL_MILLIS
                    }
                    val screenTurnedOff = withTimeoutOrNull(waitMillis) {
                        screenOff.first { it }
                    } != null
                    if (screenTurnedOff) break
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
        val intent = Intent(this, TimeBlindnessOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("minutes", minutes)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            ALERT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.time_blindness_title))
            .setContentText(getString(R.string.time_blindness_message, minutes))
            .setSmallIcon(R.drawable.timer_24px)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ALERT_NOTIFICATION_ID, notification)
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
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        SERVICE_CHANNEL_ID,
                        getString(R.string.time_blindness_service_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                    NotificationChannel(
                        ALERT_CHANNEL_ID,
                        getString(R.string.time_blindness_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = getString(R.string.time_blindness_channel_desc)
                    },
                )
            )
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.time_blindness_notification_title))
            .setContentText(getString(R.string.time_blindness_notification_text))
            .setSmallIcon(R.drawable.timer_24px)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "time_blindness_channel"
        private const val ALERT_CHANNEL_ID = "time_blindness_alerts"
        private const val NOTIFICATION_ID = 2001
        private const val ALERT_NOTIFICATION_ID = 2002
        private const val IDLE_CHECK_INTERVAL_MILLIS = 60_000L
        const val ACTION_START = "de.mm20.launcher2.action.START_TIME_BLINDNESS"
        const val ACTION_STOP = "de.mm20.launcher2.action.STOP_TIME_BLINDNESS"
    }
}

internal data class ContinuousForegroundState(
    val packageName: String? = null,
    val sinceMillis: Long? = null,
)

internal fun observeForegroundPackage(
    state: ContinuousForegroundState,
    packageName: String?,
    observedAtMillis: Long,
): ContinuousForegroundState {
    return if (state.packageName == packageName) {
        state
    } else {
        ContinuousForegroundState(packageName, observedAtMillis.takeIf { packageName != null })
    }
}
