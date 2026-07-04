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

/** Settings.Secure `accessibility_display_daltonizer` value for full monochrome (grayscale). */
internal const val DALTONIZER_MONOCHROMACY = 0

internal data class SystemGrayscaleStartDecision(
    val previousEnabledToStore: Int?,
    val previousModeToStore: Int?,
    val shouldEnableGrayscale: Boolean,
)

/**
 * Decide whether to switch the whole device to grayscale (via the Settings.Secure daltonizer)
 * when a focus session starts. Mirrors [resolveFocusDndStart]: only acts when the user opted in
 * and the launcher actually holds WRITE_SECURE_SETTINGS, and captures the pre-session daltonizer
 * state exactly once (so a re-entrant start never overwrites the real "before" value with our own).
 */
internal fun resolveSystemGrayscaleStart(
    grayscaleEnabled: Boolean,
    canWriteSecureSettings: Boolean,
    storedPreviousEnabled: Int,
    currentEnabled: Int,
    currentMode: Int,
): SystemGrayscaleStartDecision {
    if (!grayscaleEnabled || !canWriteSecureSettings) {
        return SystemGrayscaleStartDecision(
            previousEnabledToStore = null,
            previousModeToStore = null,
            shouldEnableGrayscale = false,
        )
    }
    val storeNow = storedPreviousEnabled < 0
    return SystemGrayscaleStartDecision(
        previousEnabledToStore = currentEnabled.takeIf { storeNow },
        previousModeToStore = currentMode.takeIf { storeNow },
        shouldEnableGrayscale = true,
    )
}

/**
 * Restore the device daltonizer only if we have a stored pre-session value AND the launcher still
 * owns the current state (daltonizer on + monochrome). If the user changed it themselves mid-session
 * we leave their choice alone — the direct parallel of [shouldRestorePreviousDndFilter].
 */
internal fun shouldRestoreSystemGrayscale(
    canWriteSecureSettings: Boolean,
    storedPreviousEnabled: Int,
    currentEnabled: Int,
    currentMode: Int,
): Boolean {
    return canWriteSecureSettings &&
        storedPreviousEnabled >= 0 &&
        currentEnabled == 1 &&
        currentMode == DALTONIZER_MONOCHROMACY
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
