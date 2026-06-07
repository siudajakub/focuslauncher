package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.preferences.FocusResumeContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusAssistantModelsTest {
    @Test
    fun `resolve transition warning becomes active 5 minutes before block end`() {
        val warning = resolveTransitionWarning(
            minutesUntilBlockEnd = 4,
            nextBlockLabel = "Admin",
        )

        assertTrue(warning.show)
        assertEquals("Admin", warning.nextBlockLabel)
    }

    @Test
    fun `resolve launch escalation increases only for repeated launches within cooldown window`() {
        val state = resolveLaunchEscalation(
            launchTimestamps = listOf(1_000L, 40_000L, 70_000L),
            nowMillis = 75_000L,
            windowMillis = 90_000L,
        )

        assertEquals(3, state.recentAttempts)
        assertTrue(state.extraDelaySeconds > 0)
    }

    @Test
    fun `resume card shows only if last context was interrupted recently`() {
        val card = resolveResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Writing",
                interruptedAtMillis = 100_000L,
            ),
            nowMillis = 120_000L,
            expiryMillis = 60_000L,
        )

        assertTrue(card.show)
        assertEquals("Writing", card.taskLabel)
    }

    @Test
    fun `resume card expires after timeout`() {
        val card = resolveResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Writing",
                interruptedAtMillis = 100_000L,
            ),
            nowMillis = 200_000L,
            expiryMillis = 60_000L,
        )

        assertFalse(card.show)
    }

    @Test
    fun `schedule aware resume marks current block match`() {
        val matching = resolveScheduleAwareResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Writing",
                scheduleBlockLabel = "Deep work",
                interruptedAtMillis = 100_000L,
            ),
            currentBlockLabel = "deep work",
            nowMillis = 120_000L,
            expiryMillis = 60_000L,
            followsCurrentBlock = true,
        )

        assertTrue(matching.show)
        assertTrue(matching.matchesCurrentBlock)
    }
}
