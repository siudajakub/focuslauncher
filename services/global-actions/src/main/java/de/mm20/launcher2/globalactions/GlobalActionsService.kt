package de.mm20.launcher2.globalactions

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.AlarmClock
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.core.content.getSystemService
import de.mm20.launcher2.services.focus.FocusForegroundController

class GlobalActionsService(private val context: Context) : FocusForegroundController {
    override fun isAccessibilityConnected(): Boolean {
        return LauncherAccessibilityService.getInstance() != null
    }

    fun openNotificationDrawer() {
        try {
            expandNotificationPanel()
        } catch (e: Exception) {
            LauncherAccessibilityService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    fun openQuickSettings() {
        try {
            expandQuickSettings()
        } catch (e: Exception) {
            LauncherAccessibilityService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        }
    }

    fun openPowerDialog() {
        LauncherAccessibilityService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }

    fun openRecents() {
        LauncherAccessibilityService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    override fun goHome(): Boolean {
        return LauncherAccessibilityService.getInstance()
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) == true
    }

    @Suppress("DEPRECATION")
    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun currentForegroundPackage(): String? {
        if (!hasUsageAccess()) return null
        val manager = context.getSystemService<UsageStatsManager>() ?: return null
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - FOREGROUND_LOOKBACK_MILLIS, now)
        val event = UsageEvents.Event()
        var foregroundState = ForegroundActivityState()
        val supportsActivityLifecycleEvents = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            foregroundState = reduceForegroundActivityState(
                state = foregroundState,
                eventPackage = event.packageName,
                activityId = "${event.packageName}/${event.className.orEmpty()}",
                eventTimestampMillis = event.timeStamp,
                activityResumed = supportsActivityLifecycleEvents &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED,
                // UsageEvents does not expose an Activity instance id in the public SDK. The
                // reducer therefore retires class generations on PAUSED and lets STOPPED consume
                // the oldest retired generation instead of clearing a newer resumed instance.
                activityPaused = supportsActivityLifecycleEvents &&
                    event.eventType == UsageEvents.Event.ACTIVITY_PAUSED,
                activityStopped = supportsActivityLifecycleEvents &&
                    event.eventType == UsageEvents.Event.ACTIVITY_STOPPED,
                legacyMovedToForeground = !supportsActivityLifecycleEvents &&
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND,
                legacyMovedToBackground = !supportsActivityLifecycleEvents &&
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND,
                supportsActivityLifecycleEvents = supportsActivityLifecycleEvents,
            )
        }
        return foregroundState.currentPackage()
    }

    override fun isProtectedPackage(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName in STATIC_PROTECTED_PACKAGES || packageName.contains("permissioncontroller")) return true
        val packageManager = context.packageManager
        val settingsPackage = Intent(Settings.ACTION_SETTINGS).resolveActivity(packageManager)?.packageName
        val homePackage = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .resolveActivity(packageManager)?.packageName
        val alarmPackage = Intent(AlarmClock.ACTION_SHOW_ALARMS).resolveActivity(packageManager)?.packageName
        val dialerPackage = context.getSystemService<TelecomManager>()?.defaultDialerPackage
        return packageName == settingsPackage || packageName == homePackage ||
            packageName == alarmPackage || packageName == dialerPackage
    }

    private fun expandNotificationPanel() {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    }

    private fun expandQuickSettings() {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandSettingsPanel")
        method.invoke(statusBarService)
    }

    companion object {
        private const val FOREGROUND_LOOKBACK_MILLIS = 24 * 60 * 60_000L
        private val STATIC_PROTECTED_PACKAGES = setOf(
            "android",
            "com.android.emergency",
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.emergency",
        )
    }
}
