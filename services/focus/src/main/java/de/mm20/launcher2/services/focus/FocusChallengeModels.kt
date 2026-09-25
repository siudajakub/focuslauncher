package de.mm20.launcher2.services.focus

data class FocusStepChallengeState(
    val completedSteps: Int = 0,
    val targetSteps: Int = 30,
    val lastStepAtMillis: Long? = null,
) {
    val isComplete: Boolean get() = completedSteps >= targetSteps
    val progress: Float get() = (completedSteps.toFloat() / targetSteps.coerceAtLeast(1)).coerceIn(0f, 1f)

    fun recordSteps(steps: Int, nowMillis: Long): FocusStepChallengeState {
        if (steps <= 0 || isComplete) return this
        return copy(
            completedSteps = (completedSteps + steps).coerceAtMost(targetSteps),
            lastStepAtMillis = nowMillis,
        )
    }

    fun isPaused(nowMillis: Long, pauseAfterMillis: Long = 5_000L): Boolean {
        val lastStep = lastStepAtMillis ?: return false
        return !isComplete && nowMillis - lastStep >= pauseAfterMillis
    }
}

enum class FocusUnlockGateState {
    Blocked,
    Challenge,
    ReadyToStartBreak,
    Unlocked,
}

enum class FocusUnlockGateEvent {
    ChallengeCompleted,
    StartBreak,
}

fun reduceFocusUnlockGateState(
    state: FocusUnlockGateState,
    event: FocusUnlockGateEvent,
): FocusUnlockGateState {
    return when (event) {
        FocusUnlockGateEvent.ChallengeCompleted -> if (state == FocusUnlockGateState.Challenge) {
            FocusUnlockGateState.ReadyToStartBreak
        } else {
            state
        }
        FocusUnlockGateEvent.StartBreak -> if (state == FocusUnlockGateState.ReadyToStartBreak) {
            FocusUnlockGateState.Unlocked
        } else {
            state
        }
    }
}

fun resolveChallengeDelaySeconds(
    effectiveDelaySeconds: Int,
    microDelaysEnabled: Boolean,
    minimumChallengeDelaySeconds: Int = 0,
): Int {
    val microDelayMinimum = if (microDelaysEnabled) 3 else 0
    return effectiveDelaySeconds
        .coerceAtLeast(microDelayMinimum)
        .coerceAtLeast(minimumChallengeDelaySeconds)
}

fun FocusPolicyDecision.allowsUnlockChallenge(): Boolean {
    return allowsUnlockChallenge(requiresGate, blockReason)
}

data class FocusGateResolution(
    val requiresGate: Boolean,
    val blockReason: FocusBlockReason,
)

fun resolveFocusGate(
    appType: FocusAppType,
    temporaryUnlockActive: Boolean,
    hardBlocked: Boolean,
    budgetBlocked: Boolean,
    sessionLocked: Boolean,
): FocusGateResolution {
    val gatedByClassification = appType == FocusAppType.Distracting && !temporaryUnlockActive
    val requiresGate = hardBlocked || sessionLocked || gatedByClassification
    val reason = when {
        budgetBlocked -> FocusBlockReason.DailyBudget
        hardBlocked -> FocusBlockReason.HardBlockWindow
        sessionLocked -> FocusBlockReason.FocusSessionLock
        gatedByClassification -> FocusBlockReason.Classification
        else -> FocusBlockReason.None
    }
    return FocusGateResolution(requiresGate = requiresGate, blockReason = reason)
}

fun allowsUnlockChallenge(requiresGate: Boolean, blockReason: FocusBlockReason): Boolean {
    return requiresGate && blockReason == FocusBlockReason.Classification
}

enum class FocusSystemInterceptionAction {
    Ignore,
    ScheduleExpiry,
    Intercept,
}

data class FocusPackageCandidate(
    val appKey: String,
    val isPersonalProfile: Boolean,
)

fun resolveEligiblePersonalAppKey(
    candidates: List<FocusPackageCandidate>,
    distractingAppKeys: Set<String>,
): String? {
    return resolveUniquePersonalAppKey(candidates)?.takeIf { it in distractingAppKeys }
}

fun resolveUniquePersonalAppKey(candidates: List<FocusPackageCandidate>): String? {
    if (candidates.size != 1) return null
    return candidates.single().appKey.takeIf { candidates.single().isPersonalProfile }
}

fun resolveSystemInterceptionAction(
    strictModeEffective: Boolean,
    protectedPackage: Boolean,
    requiresGate: Boolean,
    temporaryUnlockActive: Boolean,
    duplicateIntercept: Boolean,
): FocusSystemInterceptionAction {
    if (!strictModeEffective || protectedPackage) return FocusSystemInterceptionAction.Ignore
    if (!requiresGate) {
        return if (temporaryUnlockActive) {
            FocusSystemInterceptionAction.ScheduleExpiry
        } else {
            FocusSystemInterceptionAction.Ignore
        }
    }
    return if (duplicateIntercept) {
        FocusSystemInterceptionAction.Ignore
    } else {
        FocusSystemInterceptionAction.Intercept
    }
}
