package de.mm20.launcher2.ui.launcher.focus

typealias FocusAdaptiveFrictionMode = de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode

enum class FocusAdaptiveFrictionProfile {
    Light,
    Normal,
    Strict,
}

enum class FocusTimeContext {
    Morning,
    Workday,
    Bedtime,
    Neutral,
}

data class FocusEnvironmentSnapshot(
    val timeContext: FocusTimeContext = FocusTimeContext.Neutral,
    val isCharging: Boolean = false,
    val hasActiveMedia: Boolean = false,
    val isCommuteMode: Boolean = false,
    val locationLabel: String? = null,
)

enum class FocusExplanationKind {
    WorkBlock,
    PrepWindow,
    MorningMode,
    BedtimeMode,
    ChargingState,
    CommuteMode,
    ActiveMedia,
    Location,
}

data class FocusExplanationReason(
    val kind: FocusExplanationKind,
    val detail: String? = null,
)

fun resolveAdaptiveFrictionProfile(
    mode: FocusAdaptiveFrictionMode,
    repeatedDistractingLaunches: Int,
    mismatchUnlocks: Int,
    abandonedSessions: Int,
    launcherBounceCount: Int,
    repeatedBlockInterruptions: Int,
    graceWindowActive: Boolean,
): FocusAdaptiveFrictionProfile {
    return when (mode) {
        FocusAdaptiveFrictionMode.Light -> FocusAdaptiveFrictionProfile.Light
        FocusAdaptiveFrictionMode.Normal -> FocusAdaptiveFrictionProfile.Normal
        FocusAdaptiveFrictionMode.Strict -> FocusAdaptiveFrictionProfile.Strict
        FocusAdaptiveFrictionMode.Auto -> {
            if (
                graceWindowActive &&
                repeatedDistractingLaunches <= 1 &&
                mismatchUnlocks <= 1 &&
                abandonedSessions == 0 &&
                launcherBounceCount == 0 &&
                repeatedBlockInterruptions == 0
            ) {
                return FocusAdaptiveFrictionProfile.Light
            }
            val driftScore =
                repeatedDistractingLaunches * 2 +
                    mismatchUnlocks * 2 +
                    abandonedSessions * 3 +
                    launcherBounceCount +
                    repeatedBlockInterruptions * 2
            when {
                driftScore >= 10 -> FocusAdaptiveFrictionProfile.Strict
                driftScore >= 4 -> FocusAdaptiveFrictionProfile.Normal
                else -> FocusAdaptiveFrictionProfile.Light
            }
        }
    }
}

fun resolveEnvironmentReasons(
    currentBlockLabel: String?,
    prepWindowActive: Boolean,
    environment: FocusEnvironmentSnapshot,
): List<FocusExplanationReason> {
    return buildList {
        currentBlockLabel?.takeIf { it.isNotBlank() }?.let {
            add(FocusExplanationReason(FocusExplanationKind.WorkBlock, it))
        }
        if (prepWindowActive) {
            add(FocusExplanationReason(FocusExplanationKind.PrepWindow))
        }
        when (environment.timeContext) {
            FocusTimeContext.Morning -> add(FocusExplanationReason(FocusExplanationKind.MorningMode))
            FocusTimeContext.Bedtime -> add(FocusExplanationReason(FocusExplanationKind.BedtimeMode))
            FocusTimeContext.Workday,
            FocusTimeContext.Neutral,
            -> Unit
        }
        if (environment.isCharging) {
            add(FocusExplanationReason(FocusExplanationKind.ChargingState))
        }
        if (environment.hasActiveMedia) {
            add(FocusExplanationReason(FocusExplanationKind.ActiveMedia))
        }
        if (environment.isCommuteMode) {
            add(FocusExplanationReason(FocusExplanationKind.CommuteMode))
        }
        environment.locationLabel?.takeIf { it.isNotBlank() }?.let {
            add(FocusExplanationReason(FocusExplanationKind.Location, it))
        }
    }
}
