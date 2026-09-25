package de.mm20.launcher2.ui.launcher.focus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.mm20.launcher2.services.focus.FocusSystemInterceptionService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class AppSessionExpiryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val interceptionService: FocusSystemInterceptionService by inject()

    override suspend fun doWork(): Result {
        interceptionService.reconcileForeground()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, appKey: String, delayMillis: Long) {
            if (delayMillis <= 0) return

            val workManager = WorkManager.getInstance(context.applicationContext)
            val request = OneTimeWorkRequestBuilder<AppSessionExpiryWorker>()
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
