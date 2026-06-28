package de.mm20.launcher2.services.focus

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.content.getSystemService
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.FocusTemporaryUnlock
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.flow.first
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
    val temporaryUnlock: FocusTemporaryUnlock,
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

class FocusPolicyService(
    private val customAttributesRepository: CustomAttributesRepository,
    private val searchUiSettings: SearchUiSettings,
    private val profileManager: ProfileManager,
    private val historyRepository: FocusHistoryRepository,
    private val sessionRepository: FocusSessionRepository,
    private val focusAppClassifier: FocusAppClassifier,
) {

    suspend fun getProfile(app: Application): FocusTemporaryUnlock {
        return customAttributesRepository.getFocusTemporaryUnlock(app).first()
    }

    suspend fun evaluate(app: Application): FocusPolicyDecision {
        val temporaryUnlock = getProfile(app)
        val appType = focusAppClassifier.classifyNow(app.key)
        val nowMillis = System.currentTimeMillis()
        val sessionActive = searchUiSettings.focusSessionEndsAt.first() > nowMillis
        val productivityTimeActive = isProductivityTimeActive()
        val temporaryUnlockActive = temporaryUnlock.hasTemporaryUnlock()
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
                temporaryUnlock = temporaryUnlock,
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
                temporaryUnlock = temporaryUnlock,
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
                temporaryUnlock = temporaryUnlock,
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
                temporaryUnlock = temporaryUnlock,
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
        val dailyLaunchLimit = searchUiSettings.focusDistractingDailyLaunchLimit.first()
        val budgetBlocked = if (appType == FocusAppType.Distracting && dailyLaunchLimit > 0) {
            val startOfDay = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val eventsToday = historyRepository.getEventsSince(startOfDay)
            val essentialKeys = searchUiSettings.focusEssentialAppKeys.first()
            val distractingKeys = searchUiSettings.focusDistractingAppKeys.first()
            val distractingLaunchesToday = eventsToday.count {
                focusAppClassifier.classifyWith(it.appKey, essentialKeys, distractingKeys) == FocusAppType.Distracting
            }
            distractingLaunchesToday >= dailyLaunchLimit
        } else {
            false
        }
        val scheduleBlocked = false
        val sessionLocked = sessionActive && appType == FocusAppType.Distracting
        val hardBlocked = scheduleBlocked || budgetBlocked
        val gatedByClassification = appType == FocusAppType.Distracting
        val requiresGate = hardBlocked || sessionLocked || gatedByClassification
        val hiddenFromBrowse =
            appType == FocusAppType.Distracting &&
            searchUiSettings.focusHideDistractingApps.first()

        val reason = when {
            budgetBlocked -> FocusBlockReason.DailyBudget
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
            temporaryUnlock = temporaryUnlock,
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
        focusSessionMutationMutex.lock()
        try {
            val startedAt = System.currentTimeMillis()
            val until = startedAt + minutes.coerceIn(5, 180) * 60_000L
            applySessionDndStart(context)
            val session = sessionRepository.startSession(startedAt, until)
            searchUiSettings.setFocusSessionEndsAt(session.plannedEndsAt)
            FocusSessionScheduler(context).schedule(
                sessionId = session.id,
                plannedEndsAt = session.plannedEndsAt,
                now = startedAt,
            )
        } finally {
            focusSessionMutationMutex.unlock()
        }
    }

    suspend fun endFocusSession(context: Context) {
        focusSessionMutationMutex.lock()
        try {
            FocusSessionScheduler(context).cancel()
            val result = sessionRepository.endActiveSession(System.currentTimeMillis())
            if (result is FocusSessionEndResult.Finished || result is FocusSessionEndResult.NoActiveSession) {
                restoreSessionDndIfLauncherStillControls(context)
                searchUiSettings.setFocusSessionEndsAt(0L)
            }
        } finally {
            focusSessionMutationMutex.unlock()
        }
    }

    suspend fun completeScheduledFocusSession(
        context: Context,
        expectedSessionId: Long,
        expectedPlannedEndsAt: Long,
    ) {
        focusSessionMutationMutex.lock()
        try {
            val result = sessionRepository.finishExpectedSession(
                expectedSessionId = expectedSessionId,
                expectedPlannedEndsAt = expectedPlannedEndsAt,
                endedAt = System.currentTimeMillis(),
            )
            if (result is FocusSessionEndResult.Finished) {
                restoreSessionDndIfLauncherStillControls(context)
                searchUiSettings.setFocusSessionEndsAt(0L)
            }
        } finally {
            focusSessionMutationMutex.unlock()
        }
    }

    suspend fun reconcileFocusSession(context: Context) {
        focusSessionMutationMutex.lock()
        try {
            val now = System.currentTimeMillis()
            val active = sessionRepository.getActiveSession()
            when (
                val reconciliation = resolveFocusSessionReconciliation(
                    activeSessionId = active?.id,
                    activePlannedEndsAt = active?.plannedEndsAt,
                    now = now,
                )
            ) {
                is FocusSessionReconciliation.Active -> {
                    searchUiSettings.setFocusSessionEndsAt(reconciliation.projectedEndsAt)
                    FocusSessionScheduler(context).schedule(
                        sessionId = reconciliation.sessionId,
                        plannedEndsAt = reconciliation.plannedEndsAt,
                        now = now,
                    )
                }
                is FocusSessionReconciliation.Expired -> {
                    sessionRepository.finishExpectedSession(
                        expectedSessionId = reconciliation.sessionId,
                        expectedPlannedEndsAt = reconciliation.plannedEndsAt,
                        endedAt = now,
                    )
                    restoreSessionDndIfLauncherStillControls(context)
                    searchUiSettings.setFocusSessionEndsAt(0L)
                    FocusSessionScheduler(context).cancel()
                }
                FocusSessionReconciliation.NoActiveSession -> {
                    restoreSessionDndIfLauncherStillControls(context)
                    searchUiSettings.setFocusSessionEndsAt(0L)
                    FocusSessionScheduler(context).cancel()
                }
            }
        } finally {
            focusSessionMutationMutex.unlock()
        }
    }

    private suspend fun applySessionDndStart(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val decision = resolveFocusDndStart(
            dndEnabled = searchUiSettings.focusEnableDnd.first(),
            policyAccessGranted = manager.isNotificationPolicyAccessGranted,
            storedPreviousFilter = searchUiSettings.focusPreviousDndFilter.first(),
            currentFilter = manager.currentInterruptionFilter,
        )
        decision.previousFilterToStore?.let {
            searchUiSettings.setFocusPreviousDndFilter(it)
        }
        if (decision.shouldSetPriority) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    private suspend fun restoreSessionDndIfLauncherStillControls(context: Context) {
        val manager = context.getSystemService<NotificationManager>()
        val previous = searchUiSettings.focusPreviousDndFilter.first()
        if (
            manager != null &&
            shouldRestorePreviousDndFilter(
                policyAccessGranted = manager.isNotificationPolicyAccessGranted,
                storedPreviousFilter = previous,
                currentFilter = manager.currentInterruptionFilter,
                launcherFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            )
        ) {
            manager.setInterruptionFilter(previous)
        }
        searchUiSettings.setFocusPreviousDndFilter(-1)
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
