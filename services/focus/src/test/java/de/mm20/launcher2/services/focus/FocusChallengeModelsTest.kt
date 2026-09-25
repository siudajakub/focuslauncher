package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusChallengeModelsTest {
    @Test
    fun `step challenge accumulates and caps progress`() {
        val state = FocusStepChallengeState(targetSteps = 30)
            .recordSteps(12, nowMillis = 1_000L)
            .recordSteps(25, nowMillis = 2_000L)

        assertEquals(30, state.completedSteps)
        assertEquals(1f, state.progress)
        assertTrue(state.isComplete)
    }

    @Test
    fun `step challenge pauses without losing progress`() {
        val state = FocusStepChallengeState(targetSteps = 30)
            .recordSteps(7, nowMillis = 1_000L)

        assertFalse(state.isPaused(nowMillis = 5_999L))
        assertTrue(state.isPaused(nowMillis = 6_000L))
        assertEquals(7, state.completedSteps)
    }

    @Test
    fun `only classification gate allows a challenge`() {
        assertTrue(allowsUnlockChallenge(true, FocusBlockReason.Classification))
        assertFalse(allowsUnlockChallenge(true, FocusBlockReason.FocusSessionLock))
        assertFalse(allowsUnlockChallenge(true, FocusBlockReason.DailyBudget))
        assertFalse(allowsUnlockChallenge(true, FocusBlockReason.HardBlockWindow))
        assertFalse(allowsUnlockChallenge(false, FocusBlockReason.Classification))
    }

    @Test
    fun `gate routing keeps strict blocks above temporary unlock`() {
        assertEquals(
            FocusGateResolution(false, FocusBlockReason.None),
            resolveFocusGate(FocusAppType.Essential, false, false, false, false),
        )
        assertEquals(
            FocusGateResolution(false, FocusBlockReason.None),
            resolveFocusGate(FocusAppType.Normal, false, false, false, false),
        )
        assertEquals(
            FocusGateResolution(true, FocusBlockReason.Classification),
            resolveFocusGate(FocusAppType.Distracting, false, false, false, false),
        )
        assertEquals(
            FocusGateResolution(false, FocusBlockReason.None),
            resolveFocusGate(FocusAppType.Distracting, true, false, false, false),
        )
        assertEquals(
            FocusGateResolution(true, FocusBlockReason.FocusSessionLock),
            resolveFocusGate(FocusAppType.Distracting, true, false, false, true),
        )
        assertEquals(
            FocusGateResolution(true, FocusBlockReason.HardBlockWindow),
            resolveFocusGate(FocusAppType.Distracting, true, true, false, false),
        )
        assertEquals(
            FocusGateResolution(true, FocusBlockReason.DailyBudget),
            resolveFocusGate(FocusAppType.Distracting, true, true, true, false),
        )
    }

    @Test
    fun `gate requires challenge completion before starting a break`() {
        val ready = reduceFocusUnlockGateState(
            state = FocusUnlockGateState.Challenge,
            event = FocusUnlockGateEvent.ChallengeCompleted,
        )
        val unlocked = reduceFocusUnlockGateState(
            state = ready,
            event = FocusUnlockGateEvent.StartBreak,
        )

        assertEquals(FocusUnlockGateState.ReadyToStartBreak, ready)
        assertEquals(FocusUnlockGateState.Unlocked, unlocked)
        assertEquals(
            FocusUnlockGateState.Challenge,
            reduceFocusUnlockGateState(
                state = FocusUnlockGateState.Challenge,
                event = FocusUnlockGateEvent.StartBreak,
            ),
        )
    }

    @Test
    fun `step fallback enforces ten second delay`() {
        assertEquals(
            10,
            resolveChallengeDelaySeconds(
                effectiveDelaySeconds = 2,
                microDelaysEnabled = true,
                minimumChallengeDelaySeconds = 10,
            ),
        )
        assertEquals(
            18,
            resolveChallengeDelaySeconds(
                effectiveDelaySeconds = 18,
                microDelaysEnabled = false,
                minimumChallengeDelaySeconds = 10,
            ),
        )
    }

    @Test
    fun `system interception routes protected duplicate and unlocked states safely`() {
        assertEquals(
            FocusSystemInterceptionAction.Ignore,
            resolveSystemInterceptionAction(false, false, true, false, false),
        )
        assertEquals(
            FocusSystemInterceptionAction.Ignore,
            resolveSystemInterceptionAction(true, true, true, false, false),
        )
        assertEquals(
            FocusSystemInterceptionAction.ScheduleExpiry,
            resolveSystemInterceptionAction(true, false, false, true, false),
        )
        assertEquals(
            FocusSystemInterceptionAction.Ignore,
            resolveSystemInterceptionAction(true, false, true, false, true),
        )
        assertEquals(
            FocusSystemInterceptionAction.Intercept,
            resolveSystemInterceptionAction(true, false, true, false, false),
        )
    }

    @Test
    fun `system interception only resolves one distracting personal app`() {
        val distractingKeys = setOf("app:instagram")

        assertEquals(
            "app:instagram",
            resolveEligiblePersonalAppKey(
                listOf(FocusPackageCandidate("app:instagram", isPersonalProfile = true)),
                distractingKeys,
            ),
        )
        assertEquals(
            null,
            resolveEligiblePersonalAppKey(
                listOf(FocusPackageCandidate("app:instagram", isPersonalProfile = false)),
                distractingKeys,
            ),
        )
        assertEquals(
            "app:maps",
            resolveUniquePersonalAppKey(
                listOf(FocusPackageCandidate("app:maps", isPersonalProfile = true)),
            ),
        )
        assertEquals(
            null,
            resolveEligiblePersonalAppKey(
                listOf(
                    FocusPackageCandidate("app:instagram", isPersonalProfile = true),
                    FocusPackageCandidate("work:instagram", isPersonalProfile = false),
                ),
                distractingKeys,
            ),
        )
        assertEquals(
            null,
            resolveEligiblePersonalAppKey(
                listOf(FocusPackageCandidate("app:maps", isPersonalProfile = true)),
                distractingKeys,
            ),
        )
    }
}
