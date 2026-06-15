package de.mm20.launcher2.ui.launcher.focus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.LauncherActivity
import java.util.concurrent.TimeUnit

class AppSessionExpiryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appLabel = inputData.getString(INPUT_APP_LABEL) ?: return Result.failure()
        val appKey = inputData.getString(INPUT_APP_KEY) ?: return Result.failure()

        sendNotification(applicationContext, appLabel, appKey)

        return Result.success()
    }

    private fun sendNotification(context: Context, appLabel: String, appKey: String) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.focus_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.focus_notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, LauncherActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.focus_notification_expiry_title))
            .setContentText(context.getString(R.string.focus_notification_expiry_message, appLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(appKey.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "focus_productivity_reminders"
        internal const val INPUT_APP_LABEL = "appLabel"
        internal const val INPUT_APP_KEY = "appKey"

        fun schedule(context: Context, appKey: String, appLabel: String, delayMillis: Long) {
            if (delayMillis <= 0) return

            val workManager = WorkManager.getInstance(context.applicationContext)
            val inputData = Data.Builder()
                .putString(INPUT_APP_KEY, appKey)
                .putString(INPUT_APP_LABEL, appLabel)
                .build()

            val request = OneTimeWorkRequestBuilder<AppSessionExpiryWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                "AppSessionExpiry_$appKey",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, appKey: String) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork("AppSessionExpiry_$appKey")
        }
    }
}
