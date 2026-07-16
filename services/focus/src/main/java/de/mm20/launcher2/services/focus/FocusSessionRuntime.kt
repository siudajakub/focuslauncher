package de.mm20.launcher2.services.focus

import kotlinx.coroutines.sync.Mutex

internal val focusSessionMutationMutex = Mutex()

internal data class FocusDndStartDecision(
    val previousFilterToStore: Int?,
    val shouldSetPriority: Boolean,
)

internal fun resolveFocusDndStart(
    dndEnabled: Boolean,
    policyAccessGranted: Boolean,
    storedPreviousFilter: Int,
    currentFilter: Int,
): FocusDndStartDecision {
    if (!dndEnabled || !policyAccessGranted) {
        return FocusDndStartDecision(
            previousFilterToStore = null,
            shouldSetPriority = false,
        )
    }
    return FocusDndStartDecision(
        previousFilterToStore = currentFilter.takeIf { storedPreviousFilter < 0 },
        shouldSetPriority = true,
    )
}

internal fun shouldRestorePreviousDndFilter(
    policyAccessGranted: Boolean,
    storedPreviousFilter: Int,
    currentFilter: Int,
    launcherFilter: Int,
): Boolean {
    return policyAccessGranted &&
        storedPreviousFilter >= 0 &&
        currentFilter == launcherFilter
}

/**
 * True if [nowMinutesOfDay] (0..1439) falls in the daily window `[startMinutes, endMinutes)`.
 * Windows that wrap past midnight (start >= end, e.g. an evening wind-down 20:00–08:00) are handled.
 * A zero-length window (start == end) is treated as "off". Pure so callers on any thread/module can
 * evaluate a schedule without pulling in `LocalDateTime`.
 */
fun isWithinDailyWindow(startMinutes: Int, endMinutes: Int, nowMinutesOfDay: Int): Boolean {
    if (startMinutes == endMinutes) return false
    return if (startMinutes < endMinutes) {
        nowMinutesOfDay in startMinutes until endMinutes
    } else {
        nowMinutesOfDay >= startMinutes || nowMinutesOfDay < endMinutes
    }
}

internal fun isFocusSessionActive(plannedEndsAt: Long?, now: Long): Boolean {
    return plannedEndsAt != null && plannedEndsAt > now
}

internal fun isExpectedFocusSession(
    activeSessionId: Long,
    activePlannedEndsAt: Long,
    expectedSessionId: Long,
    expectedPlannedEndsAt: Long,
): Boolean {
    return activeSessionId == expectedSessionId &&
        activePlannedEndsAt == expectedPlannedEndsAt
}

internal sealed interface FocusSessionReconciliation {
    val projectedEndsAt: Long

    data object NoActiveSession : FocusSessionReconciliation {
        override val projectedEndsAt: Long = 0L
    }

    data class Active(
        val sessionId: Long,
        val plannedEndsAt: Long,
    ) : FocusSessionReconciliation {
        override val projectedEndsAt: Long = plannedEndsAt
    }

    data class Expired(
        val sessionId: Long,
        val plannedEndsAt: Long,
    ) : FocusSessionReconciliation {
        override val projectedEndsAt: Long = 0L
    }
}

internal fun resolveFocusSessionReconciliation(
    activeSessionId: Long?,
    activePlannedEndsAt: Long?,
    now: Long,
): FocusSessionReconciliation {
    if (activeSessionId == null || activePlannedEndsAt == null) {
        return FocusSessionReconciliation.NoActiveSession
    }
    return if (activePlannedEndsAt > now) {
        FocusSessionReconciliation.Active(activeSessionId, activePlannedEndsAt)
    } else {
        FocusSessionReconciliation.Expired(activeSessionId, activePlannedEndsAt)
    }
}
