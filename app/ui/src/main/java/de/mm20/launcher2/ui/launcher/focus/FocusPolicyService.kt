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

enum class FocusBlockReason {
    None,
    HardBlockWindow,
    HabitDeadline,
    DailyBudget,
    FocusSessionLock,
    Classification,
}

data class FocusFrictionResolution(
    val mode: FocusAdaptiveFrictionMode,
    val profile: FocusAdaptiveFrictionProfile,
    val baseDelaySeconds: Int,
    val launchAttemptCount: Int,
    val launchEscalationDelaySeconds: Int,
    val attentionDelaySeconds: Int,
    val resolvedDelaySeconds: Int,
    val graceWindowActive: Boolean,
    val driftScore: Int,
    val signals: List<FocusAttentionSignal>,
)

data class FocusPolicyDecision(
    val appType: FocusAppType,
    val profile: FocusProfile,
    val requiresGate: Boolean,
    val hiddenFromBrowse: Boolean,
    val hardBlocked: Boolean,
    val budgetBlocked: Boolean,
    val focusSessionLocked: Boolean,
    val habitBlocked: Boolean,
    val temporaryUnlockActive: Boolean,
    val effectiveDelaySeconds: Int,
    val blockReason: FocusBlockReason,
    val blockingHabitTitle: String?,
    val attentionState: FocusAttentionState,
    val frictionResolution: FocusFrictionResolution,
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
        val nowMillis = System.currentTimeMillis()
        val sessionActive = searchUiSettings.focusSessionEndsAt.first() > nowMillis
        val productivityTimeActive = isProductivityTimeActive()
        val temporaryUnlockActive = profile.hasTemporaryUnlock()
        val defaultDelaySeconds = searchUiSettings.focusDefaultDelaySeconds.first()
        val escalatingFrictionEnabled = searchUiSettings.focusEscalatingFrictionEnabled.first()
        val escalationWindowMinutes = searchUiSettings.focusEscalationWindowMinutes.first()
        val escalationExtraDelaySeconds = searchUiSettings.focusEscalationExtraDelaySeconds.first()
        val protectionWindowMillis = escalationWindowMinutes * 60_000L
        val recentLaunchEscalation = if (appType == FocusAppType.Distracting && escalatingFrictionEnabled) {
            resolveLaunchEscalation(
                launchTimestamps = historyRepository.getRecentAppLaunchTimestamps(
                    appKey = app.key,
                    sinceMillis = nowMillis - protectionWindowMillis,
                ),
                nowMillis = nowMillis,
                windowMillis = protectionWindowMillis,
                baseExtraDelaySeconds = escalationExtraDelaySeconds,
            )
        } else {
            LaunchEscalationState(recentAttempts = 0, extraDelaySeconds = 0)
        }
        val attentionState = if (appType == FocusAppType.Distracting) {
            historyRepository.getAttentionStateForApp(
                appKey = app.key,
                sinceMillis = nowMillis - protectionWindowMillis,
                nowMillis = nowMillis,
            )
        } else {
            FocusAttentionState.idle(
                appKey = app.key,
                windowStartMillis = nowMillis - protectionWindowMillis,
                windowEndMillis = nowMillis,
            )
        }
        val frictionResolution = resolveFrictionResolution(
            mode = searchUiSettings.focusAdaptiveFrictionMode.first(),
            baseDelaySeconds = defaultDelaySeconds,
            escalatingFrictionEnabled = escalatingFrictionEnabled,
            attentionState = attentionState,
            launchAttemptCount = recentLaunchEscalation.recentAttempts,
            launchEscalationDelaySeconds = recentLaunchEscalation.extraDelaySeconds,
        )
        if (!shouldApplyToProfile(app)) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = false,
                hiddenFromBrowse = false,
                hardBlocked = false,
                budgetBlocked = false,
                focusSessionLocked = false,
                habitBlocked = false,
                temporaryUnlockActive = temporaryUnlockActive,
                effectiveDelaySeconds = 0,
                blockReason = FocusBlockReason.None,
                blockingHabitTitle = null,
                attentionState = attentionState,
                frictionResolution = frictionResolution,
            )
        }
        if (productivityTimeActive && appType == FocusAppType.Distracting) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = true,
                hiddenFromBrowse = searchUiSettings.focusHideDistractingApps.first(),
                hardBlocked = true,
                budgetBlocked = false,
                focusSessionLocked = false,
                habitBlocked = false,
                temporaryUnlockActive = temporaryUnlockActive,
                effectiveDelaySeconds = frictionResolution.resolvedDelaySeconds.coerceAtLeast(defaultDelaySeconds),
                blockReason = FocusBlockReason.HardBlockWindow,
                blockingHabitTitle = null,
                attentionState = attentionState,
                frictionResolution = frictionResolution,
            )
        }
        val habitGate = if (appType == FocusAppType.Distracting && searchUiSettings.focusHabitsEnabled.first()) {
            resolveHabitGate(
                habits = searchUiSettings.focusHabits.first(),
                today = LocalDate.now(),
                now = LocalDateTime.now(),
            )
        } else {
            HabitGateState(blocked = false, overdueCount = 0)
        }
        if (habitGate.blocked) {
            return FocusPolicyDecision(
                appType = appType,
                profile = profile,
                requiresGate = true,
                hiddenFromBrowse = searchUiSettings.focusHideDistractingApps.first(),
                hardBlocked = true,
                budgetBlocked = false,
                focusSessionLocked = false,
                habitBlocked = true,
                temporaryUnlockActive = false,
                effectiveDelaySeconds = frictionResolution.resolvedDelaySeconds.coerceAtLeast(defaultDelaySeconds),
                blockReason = FocusBlockReason.HabitDeadline,
                blockingHabitTitle = habitGate.primaryOverdueHabitTitle,
                attentionState = attentionState,
                frictionResolution = frictionResolution,
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
                habitBlocked = false,
                temporaryUnlockActive = true,
                effectiveDelaySeconds = 0,
                blockReason = FocusBlockReason.None,
                blockingHabitTitle = null,
                attentionState = attentionState,
                frictionResolution = frictionResolution,
            )
        }
        val scheduleBlocked = false
        val budgetBlocked = false
        val sessionLocked = sessionActive && appType == FocusAppType.Distracting
        val hardBlocked = scheduleBlocked || budgetBlocked
        val gatedByClassification = appType == FocusAppType.Distracting
        val requiresGate = hardBlocked || sessionLocked || gatedByClassification
        val hiddenFromBrowse =
            appType == FocusAppType.Distracting &&
            searchUiSettings.focusHideDistractingApps.first()

        val reason = when {
            hardBlocked -> FocusBlockReason.HardBlockWindow
            sessionLocked -> FocusBlockReason.FocusSessionLock
            gatedByClassification -> FocusBlockReason.Classification
            else -> FocusBlockReason.None
        }

        val effectiveDelay = when {
            appType != FocusAppType.Distracting -> 0
            temporaryUnlockActive -> 0
            hardBlocked -> frictionResolution.resolvedDelaySeconds.coerceAtLeast(defaultDelaySeconds)
            sessionLocked -> frictionResolution.resolvedDelaySeconds
            escalatingFrictionEnabled -> frictionResolution.resolvedDelaySeconds
            else -> defaultDelaySeconds
        }

        return FocusPolicyDecision(
            appType = appType,
            profile = profile,
            requiresGate = requiresGate,
            hiddenFromBrowse = hiddenFromBrowse,
            hardBlocked = hardBlocked,
            budgetBlocked = budgetBlocked,
            focusSessionLocked = sessionLocked,
            habitBlocked = false,
            temporaryUnlockActive = false,
            effectiveDelaySeconds = effectiveDelay,
            blockReason = reason,
            blockingHabitTitle = null,
            attentionState = attentionState,
            frictionResolution = frictionResolution,
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
        return searchUiSettings.focusProductivityWindows.first().any { window ->
            isWithinWindow(
                startMinutes = window.startMinutes,
                endMinutes = window.endMinutes,
                now = now,
            )
        }
    }

    private fun isWithinWindow(startMinutes: Int, endMinutes: Int, now: LocalDateTime): Boolean {
        val current = now.hour * 60 + now.minute
        return if (startMinutes < endMinutes) {
            current in startMinutes until endMinutes
        } else {
            current >= startMinutes || current < endMinutes
        }
    }

    private fun resolveFrictionResolution(
        mode: FocusAdaptiveFrictionMode,
        baseDelaySeconds: Int,
        escalatingFrictionEnabled: Boolean,
        attentionState: FocusAttentionState,
        launchAttemptCount: Int,
        launchEscalationDelaySeconds: Int,
    ): FocusFrictionResolution {
        if (!escalatingFrictionEnabled) {
            val staticProfile = when (mode) {
                FocusAdaptiveFrictionMode.Light -> FocusAdaptiveFrictionProfile.Light
                FocusAdaptiveFrictionMode.Normal -> FocusAdaptiveFrictionProfile.Normal
                FocusAdaptiveFrictionMode.Strict -> FocusAdaptiveFrictionProfile.Strict
                FocusAdaptiveFrictionMode.Auto -> FocusAdaptiveFrictionProfile.Light
            }
            return FocusFrictionResolution(
                mode = mode,
                profile = staticProfile,
                baseDelaySeconds = baseDelaySeconds,
                launchAttemptCount = 0,
                launchEscalationDelaySeconds = 0,
                attentionDelaySeconds = 0,
                resolvedDelaySeconds = when (staticProfile) {
                    FocusAdaptiveFrictionProfile.Light -> (baseDelaySeconds / 2).coerceAtLeast(0)
                    FocusAdaptiveFrictionProfile.Normal -> baseDelaySeconds
                    FocusAdaptiveFrictionProfile.Strict -> baseDelaySeconds + (baseDelaySeconds / 2)
                }.coerceIn(0, 120),
                graceWindowActive = true,
                driftScore = attentionState.driftScore,
                signals = attentionState.signals,
            )
        }

        val profile = resolveAdaptiveFrictionProfile(
            mode = mode,
            repeatedDistractingLaunches = attentionState.recentUnlockCount + attentionState.sameAppRepeatCount,
            mismatchUnlocks = attentionState.mismatchUnlockCount + attentionState.missedHabitSignalCount,
            abandonedSessions = attentionState.abandonedSessionCount,
            launcherBounceCount = attentionState.launcherBounceCount,
            repeatedBlockInterruptions = attentionState.repeatedBlockInterruptionCount,
            graceWindowActive = attentionState.graceWindowActive,
        )
        val attentionDelaySeconds = when (profile) {
            FocusAdaptiveFrictionProfile.Light -> (attentionState.driftScore / 2).coerceAtLeast(0)
            FocusAdaptiveFrictionProfile.Normal -> attentionState.driftScore.coerceAtLeast(0)
            FocusAdaptiveFrictionProfile.Strict -> (attentionState.driftScore * 2).coerceAtLeast(0)
        }
        val profileDelaySeconds = when (profile) {
            FocusAdaptiveFrictionProfile.Light -> baseDelaySeconds / 2
            FocusAdaptiveFrictionProfile.Normal -> baseDelaySeconds
            FocusAdaptiveFrictionProfile.Strict -> baseDelaySeconds + attentionDelaySeconds
        }
        val resolvedDelaySeconds =
            (profileDelaySeconds + attentionDelaySeconds + launchEscalationDelaySeconds).coerceIn(0, 120)

        return FocusFrictionResolution(
            mode = mode,
            profile = profile,
            baseDelaySeconds = baseDelaySeconds,
            launchAttemptCount = launchAttemptCount,
            launchEscalationDelaySeconds = launchEscalationDelaySeconds,
            attentionDelaySeconds = attentionDelaySeconds,
            resolvedDelaySeconds = resolvedDelaySeconds,
            graceWindowActive = attentionState.graceWindowActive,
            driftScore = attentionState.driftScore,
            signals = attentionState.signals,
        )
    }
}
