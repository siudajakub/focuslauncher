package de.mm20.launcher2.ui.launcher.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Manifest-declared receiver. Only ACTION_BOOT_COMPLETED is deliverable here on
 * modern Android. SCREEN_OFF / USER_PRESENT are handled by a runtime-registered
 * receiver inside [TimeBlindnessService].
 *
 * On boot we simply (re)start the service. The service re-checks the
 * `focusTimeBlindnessRemindersEnabled` setting in onStartCommand and stops
 * itself immediately if the feature is disabled, so this receiver does not need
 * (and cannot synchronously) read the DataStore here.
 */
class TimeBlindnessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val serviceIntent = Intent(context, TimeBlindnessService::class.java).apply {
            action = TimeBlindnessService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
