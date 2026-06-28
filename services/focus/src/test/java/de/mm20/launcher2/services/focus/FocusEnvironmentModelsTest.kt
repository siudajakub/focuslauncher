package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusEnvironmentModelsTest {
    @Test
    fun `adaptive friction becomes strict after repeated drift`() {
        val profile = resolveAdaptiveFrictionProfile(
            mode = FocusAdaptiveFrictionMode.Auto,
            repeatedDistractingLaunches = 4,
            mismatchUnlocks = 3,
            abandonedSessions = 1,
            launcherBounceCount = 2,
            repeatedBlockInterruptions = 3,
            graceWindowActive = false,
        )

        assertEquals(FocusAdaptiveFrictionProfile.Strict, profile)
    }

    @Test
    fun `adaptive friction stays light during grace window after one detour`() {
        val profile = resolveAdaptiveFrictionProfile(
            mode = FocusAdaptiveFrictionMode.Auto,
            repeatedDistractingLaunches = 1,
            mismatchUnlocks = 1,
            abandonedSessions = 0,
            launcherBounceCount = 0,
            repeatedBlockInterruptions = 0,
            graceWindowActive = true,
        )

        assertEquals(FocusAdaptiveFrictionProfile.Light, profile)
    }

    @Test
    fun `environment reasons include prep and bedtime context`() {
        val reasons = resolveEnvironmentReasons(
            currentBlockLabel = "Writing",
            prepWindowActive = true,
            environment = FocusEnvironmentSnapshot(
                timeContext = FocusTimeContext.Bedtime,
                isCharging = true,
            ),
        )

        assertTrue(reasons.any { it.kind == FocusExplanationKind.WorkBlock })
        assertTrue(reasons.any { it.kind == FocusExplanationKind.PrepWindow })
        assertTrue(reasons.any { it.kind == FocusExplanationKind.BedtimeMode })
        assertTrue(reasons.any { it.kind == FocusExplanationKind.ChargingState })
    }
}
