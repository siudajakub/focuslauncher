package de.mm20.launcher2.services.focus

import android.content.Context
import android.os.Process
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class FocusSystemInterceptionService(
    private val context: Context,
    private val appRepository: AppRepository,
    private val searchUiSettings: SearchUiSettings,
    private val focusPolicyService: FocusPolicyService,
    private val gateLauncher: FocusGateLauncher,
    private val foregroundController: FocusForegroundController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val interceptionMutex = Mutex()
    private val expiryJobs = ConcurrentHashMap<String, Job>()
    private val configuredEnabled = MutableStateFlow<Boolean?>(null)
    private var lastInterceptedPackage: String? = null
    private var lastInterceptedAtMillis: Long = 0L

    init {
        scope.launch {
            combine(
                searchUiSettings.focusModeEnabled,
                searchUiSettings.focusSystemInterceptionEnabled,
            ) { focusEnabled, interceptionEnabled ->
                focusEnabled && interceptionEnabled
            }.collect { configuredEnabled.value = it }
        }
    }

    fun shouldHandleForegroundEvents(): Boolean = configuredEnabled.value != false

    suspend fun onForegroundPackage(packageName: String) {
        if (!shouldHandleForegroundEvents()) return
        interceptionMutex.withLock {
            val strictModeEffective = isStrictModeEffective()
            if (!strictModeEffective) return
            val app = resolveEligiblePersonalApp(packageName) ?: return
            val protectedPackage = foregroundController.isProtectedPackage(packageName)
            if (protectedPackage) return
            val decision = focusPolicyService.evaluate(app)
            val now = System.currentTimeMillis()
            val duplicateIntercept = packageName == lastInterceptedPackage &&
                now - lastInterceptedAtMillis < INTERCEPTION_DEBOUNCE_MILLIS
            when (
                resolveSystemInterceptionAction(
                    strictModeEffective = strictModeEffective,
                    protectedPackage = protectedPackage,
                    requiresGate = decision.requiresGate,
                    temporaryUnlockActive = decision.temporaryUnlockActive,
                    duplicateIntercept = duplicateIntercept,
                )
            ) {
                FocusSystemInterceptionAction.Ignore -> return
                FocusSystemInterceptionAction.ScheduleExpiry -> {
                    scheduleExpiry(app, decision.temporaryUnlock.untilMillis)
                    return
                }
                FocusSystemInterceptionAction.Intercept -> Unit
            }

            lastInterceptedPackage = packageName
            lastInterceptedAtMillis = now
            expiryJobs.remove(app.key)?.cancel()

            withContext(Dispatchers.Main.immediate) {
                if (foregroundController.goHome()) {
                    delay(GATE_AFTER_HOME_DELAY_MILLIS)
                    gateLauncher.openGate(context, app, null)
                }
            }
        }
    }

    suspend fun reconcileForeground() {
        if (!isStrictModeEffective()) return
        val packageName = foregroundController.currentForegroundPackage() ?: return
        onForegroundPackage(packageName)
    }

    fun scheduleExpiry(app: Application, untilMillis: Long) {
        expiryJobs.remove(app.key)?.cancel()
        val remainingMillis = untilMillis - System.currentTimeMillis()
        if (remainingMillis <= 0L) return
        val expiryJob = scope.launch(start = CoroutineStart.LAZY) {
            delay(remainingMillis)
            coroutineContext[Job]?.let { completedJob ->
                expiryJobs.remove(app.key, completedJob)
            }
            reconcileForeground()
        }
        expiryJobs[app.key] = expiryJob
        expiryJob.start()
    }

    private suspend fun isStrictModeEffective(): Boolean {
        val enabled = configuredEnabled.value ?: (
            searchUiSettings.focusModeEnabled.first() &&
                searchUiSettings.focusSystemInterceptionEnabled.first()
            )
        return enabled &&
            foregroundController.isAccessibilityConnected() &&
            foregroundController.hasUsageAccess()
    }

    private suspend fun resolveEligiblePersonalApp(packageName: String): Application? {
        val apps = appRepository.findMany().first().filter {
            it.componentName.packageName == packageName
        }
        val distractingKeys = searchUiSettings.focusDistractingAppKeys.first()
        val resolvedKey = resolveEligiblePersonalAppKey(
            candidates = apps.map {
                FocusPackageCandidate(
                    appKey = it.key,
                    isPersonalProfile = it.user == Process.myUserHandle(),
                )
            },
            distractingAppKeys = distractingKeys,
        ) ?: return null
        return apps.single { it.key == resolvedKey }
    }

    companion object {
        private const val INTERCEPTION_DEBOUNCE_MILLIS = 1_000L
        private const val GATE_AFTER_HOME_DELAY_MILLIS = 180L
    }
}
