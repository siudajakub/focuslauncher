package de.mm20.launcher2.services.focus

import de.mm20.launcher2.preferences.FocusResumeContext

data class TransitionWarningState(
    val show: Boolean,
    val nextBlockLabel: String? = null,
)

data class LaunchEscalationState(
    val recentAttempts: Int,
    val extraDelaySeconds: Int,
)

data class ResumeCardState(
    val show: Boolean,
    val taskLabel: String? = null,
    val microStep: String? = null,
    val relatedBlockLabel: String? = null,
    val matchesCurrentBlock: Boolean = false,
)

fun resolveTransitionWarning(
    minutesUntilBlockEnd: Int,
    nextBlockLabel: String?,
    leadMinutes: Int = 5,
): TransitionWarningState {
    val shouldShow = nextBlockLabel != null && minutesUntilBlockEnd in 0..leadMinutes
    return TransitionWarningState(
        show = shouldShow,
        nextBlockLabel = nextBlockLabel,
    )
}

fun resolveLaunchEscalation(
    launchTimestamps: List<Long>,
    nowMillis: Long,
    windowMillis: Long,
    baseExtraDelaySeconds: Int = 10,
): LaunchEscalationState {
    val windowStart = nowMillis - windowMillis
    val recentAttempts = launchTimestamps.count { it in windowStart..nowMillis }
    val extraDelaySeconds = when {
        recentAttempts <= 1 -> 0
        recentAttempts == 2 -> baseExtraDelaySeconds
        else -> baseExtraDelaySeconds * 2
    }
    return LaunchEscalationState(
        recentAttempts = recentAttempts,
        extraDelaySeconds = extraDelaySeconds,
    )
}

fun resolveResumeCard(
    lastContext: FocusResumeContext?,
    nowMillis: Long,
    expiryMillis: Long,
): ResumeCardState {
    if (lastContext == null) return ResumeCardState(show = false)
    val isExpired = nowMillis - lastContext.interruptedAtMillis > expiryMillis
    if (isExpired) return ResumeCardState(show = false)
    return ResumeCardState(
        show = true,
        taskLabel = lastContext.taskLabel,
        microStep = lastContext.microStep,
        relatedBlockLabel = lastContext.scheduleBlockLabel,
    )
}
