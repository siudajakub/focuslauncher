package de.mm20.launcher2.services.focus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class FocusSessionExpiryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(INPUT_SESSION_ID, INVALID_SESSION_ID)
        val plannedEndsAt = inputData.getLong(INPUT_PLANNED_ENDS_AT, 0L)
        if (sessionId == INVALID_SESSION_ID || plannedEndsAt <= 0L) {
            return Result.failure()
        }

        FocusPolicyService().completeScheduledFocusSession(
            context = applicationContext,
            expectedSessionId = sessionId,
            expectedPlannedEndsAt = plannedEndsAt,
        )
        return Result.success()
    }

    companion object {
        internal const val UNIQUE_WORK_NAME = "FocusSessionExpiry"
        internal const val INPUT_SESSION_ID = "sessionId"
        internal const val INPUT_PLANNED_ENDS_AT = "plannedEndsAt"
        private const val INVALID_SESSION_ID = -1L
    }
}

internal class FocusSessionScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(sessionId: Long, plannedEndsAt: Long, now: Long = System.currentTimeMillis()) {
        val inputData = Data.Builder()
            .putLong(FocusSessionExpiryWorker.INPUT_SESSION_ID, sessionId)
            .putLong(FocusSessionExpiryWorker.INPUT_PLANNED_ENDS_AT, plannedEndsAt)
            .build()
        val request = OneTimeWorkRequestBuilder<FocusSessionExpiryWorker>()
            .setInputData(inputData)
            .setInitialDelay((plannedEndsAt - now).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            FocusSessionExpiryWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(FocusSessionExpiryWorker.UNIQUE_WORK_NAME)
    }
}
