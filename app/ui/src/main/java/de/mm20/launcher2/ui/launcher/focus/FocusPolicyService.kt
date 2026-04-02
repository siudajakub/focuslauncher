package de.mm20.launcher2.ui.launcher.focus

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.FocusProfile
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class FocusBlockReason {
    None,
    HardBlockWindow,
    DailyBudget,
    FocusSessionLock,
    Classification,
}

data class FocusPolicyDecision(
    val appType: FocusAppType,
    val profile: FocusProfile,
    val requiresGate: Boolean,
    val hiddenFromBrowse: Boolean,
    val hardBlocked: Boolean,
    val budgetBlocked: Boolean,
    val focusSessionLocked: Boolean,
    val temporaryUnlockActive: Boolean,
    val emergencyBypassActive: Boolean,
    val bypassAllowed: Boolean,
    val effectiveDelaySeconds: Int,
    val blockReason: FocusBlockReason,
)

class FocusPolicyService : KoinComponent {
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val profileManager: ProfileManager by inject()
    private val historyRepository = FocusHistoryRepository()
    private val sessionRepository = FocusSessionRepository()
    private val focusAppClassifier = FocusAppClassifier()

    suspend fun getProfile(app: Application): FocusProfile {
        return customAttributesRepository.getFocusProfile(app).first()
    }

    suspend fun evaluate(app: Application): FocusPolicyDecision {
        val profile = getProfile(app)
        val appType = focusAppClassifier.classifyNow(app.key)
        val sessionActive = searchUiSettings.focusSessionEndsAt.first() > System.currentTimeMillis()
        val emergencyBypassActive = searchUiSettings.focusEmergencyBypassEndsAt.first() > System.currentTimeMillis()
        val productivityTimeActive = isProductivityTimeActive()
        val temporaryUnlockActive = profile.hasTemporaryUnlock()
        if (!shouldApplyToProfile(app)) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = false,
                hiddenFromBrowse = false,
                hardBlocked = false,
                budgetBlocked = false,
                focusSessionLocked = false,
                temporaryUnlockActive = temporaryUnlockActive,
                emergencyBypassActive = emergencyBypassActive,
                bypassAllowed = profile.allowEmergencyBypass,
                effectiveDelaySeconds = 0,
                blockReason = FocusBlockReason.None,
            )
        }
        if (productivityTimeActive && !emergencyBypassActive && appType == FocusAppType.Distracting) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = true,
                hiddenFromBrowse = searchUiSettings.focusHideDistractingApps.first(),
                hardBlocked = true,
                budgetBlocked = false,
                focusSessionLocked = false,
                temporaryUnlockActive = temporaryUnlockActive,
                emergencyBypassActive = emergencyBypassActive,
                bypassAllowed = profile.allowEmergencyBypass,
                effectiveDelaySeconds = 0,
                blockReason = FocusBlockReason.HardBlockWindow,
            )
        }
        if (temporaryUnlockActive) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = false,
                hiddenFromBrowse = false,
                hardBlocked = false,
                budgetBlocked = false,
                focusSessionLocked = false,
                temporaryUnlockActive = true,
                emergencyBypassActive = emergencyBypassActive,
                bypassAllowed = profile.allowEmergencyBypass,
                effectiveDelaySeconds = 0,
                blockReason = FocusBlockReason.None,
            )
        }
        val effectiveDelay = if (appType == FocusAppType.Distracting) {
            searchUiSettings.focusDefaultDelaySeconds.first()
        } else {
            0
        }

        val scheduleBlocked = false
        val budgetBlocked = false
        val sessionLocked = sessionActive && appType == FocusAppType.Distracting
        val hardBlocked = !emergencyBypassActive && (scheduleBlocked || budgetBlocked)
        val gatedByClassification = appType == FocusAppType.Distracting
        val requiresGate = !emergencyBypassActive && (hardBlocked || sessionLocked || gatedByClassification)
        val hiddenFromBrowse = !emergencyBypassActive &&
            appType == FocusAppType.Distracting &&
            searchUiSettings.focusHideDistractingApps.first()

        val reason = when {
            hardBlocked -> FocusBlockReason.HardBlockWindow
            sessionLocked -> FocusBlockReason.FocusSessionLock
            gatedByClassification -> FocusBlockReason.Classification
            else -> FocusBlockReason.None
        }

        return FocusPolicyDecision(
            appType = appType,
            profile = profile,
            requiresGate = requiresGate,
            hiddenFromBrowse = hiddenFromBrowse,
            hardBlocked = hardBlocked,
            budgetBlocked = budgetBlocked,
            focusSessionLocked = sessionLocked,
            temporaryUnlockActive = false,
            emergencyBypassActive = emergencyBypassActive,
            bypassAllowed = profile.allowEmergencyBypass,
            effectiveDelaySeconds = effectiveDelay,
            blockReason = reason,
        )
    }

    suspend fun beginFocusSession(context: Context, minutes: Int) {
        val startedAt = System.currentTimeMillis()
        val until = startedAt + minutes.coerceIn(5, 180) * 60_000L
        if (searchUiSettings.focusEnableDnd.first()) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null && manager.isNotificationPolicyAccessGranted) {
                searchUiSettings.setFocusPreviousDndFilter(manager.currentInterruptionFilter)
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        }
        sessionRepository.startSession(startedAt, until)
        searchUiSettings.setFocusSessionEndsAt(until)
    }

    suspend fun endFocusSession(context: Context) {
        val endedAt = System.currentTimeMillis()
        val manager = context.getSystemService(NotificationManager::class.java)
        val previous = searchUiSettings.focusPreviousDndFilter.first()
        if (manager != null && manager.isNotificationPolicyAccessGranted && previous >= 0) {
            manager.setInterruptionFilter(previous)
        }
        searchUiSettings.setFocusPreviousDndFilter(-1)
        searchUiSettings.setFocusSessionEndsAt(0L)
        sessionRepository.endActiveSession(endedAt)
    }

    suspend fun activateEmergencyBypass(reason: String, minutes: Int) {
        val until = System.currentTimeMillis() + minutes.coerceIn(1, 60) * 60_000L
        searchUiSettings.setFocusEmergencyBypassReason(reason)
        searchUiSettings.setFocusEmergencyBypassEndsAt(until)
    }

    suspend fun clearEmergencyBypass() {
        searchUiSettings.setFocusEmergencyBypassReason(null)
        searchUiSettings.setFocusEmergencyBypassEndsAt(0L)
    }

    fun getDndSettingsIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private suspend fun shouldApplyToProfile(app: Application): Boolean {
        return when (profileManager.getProfile(app.user).first()?.type ?: Profile.Type.Personal) {
            Profile.Type.Personal -> searchUiSettings.focusApplyToPersonalProfile.first()
            Profile.Type.Work -> searchUiSettings.focusApplyToWorkProfile.first()
            Profile.Type.Private -> searchUiSettings.focusApplyToPrivateProfile.first()
        }
    }

    private suspend fun isProductivityTimeActive(): Boolean {
        if (!searchUiSettings.focusProductivityTimeEnabled.first()) return false
        val now = LocalDateTime.now()
        return isWithinWindow(
            startMinutes = searchUiSettings.focusProductivityWindow1StartMinutes.first(),
            endMinutes = searchUiSettings.focusProductivityWindow1EndMinutes.first(),
            now = now,
        ) || isWithinWindow(
            startMinutes = searchUiSettings.focusProductivityWindow2StartMinutes.first(),
            endMinutes = searchUiSettings.focusProductivityWindow2EndMinutes.first(),
            now = now,
        )
    }

    private fun isWithinWindow(startMinutes: Int, endMinutes: Int, now: LocalDateTime): Boolean {
        val current = now.hour * 60 + now.minute
        return if (startMinutes < endMinutes) {
            current in startMinutes until endMinutes
        } else {
            current >= startMinutes || current < endMinutes
        }
    }
}
