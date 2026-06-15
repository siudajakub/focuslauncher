package de.mm20.launcher2.ui.launcher.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TimeBlindnessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, TimeBlindnessService::class.java)
        if (action == Intent.ACTION_USER_PRESENT || action == Intent.ACTION_BOOT_COMPLETED) {
            serviceIntent.action = TimeBlindnessService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else if (action == Intent.ACTION_SCREEN_OFF) {
            serviceIntent.action = TimeBlindnessService.ACTION_STOP
            context.startService(serviceIntent)
        }
    }
}
