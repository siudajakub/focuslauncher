package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.entities.FocusEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyFocusDeltaTest {
    @Test
    fun `delta compares current and previous week`() {
        val currentEvents = listOf(
            focusEvent("a", "Video"),
            focusEvent("a", "Video"),
        )
        val previousEvents = listOf(
            focusEvent("a", "Video"),
        )

        val delta = computeWeeklyDelta(
            currentEvents = currentEvents,
            previousEvents = previousEvents,
            currentSessionMinutes = 40,
            previousSessionMinutes = 25,
        )

        assertEquals(1, delta.unlocksDelta)
        assertEquals(15, delta.sessionMinutesDelta)
        assertEquals("Video", delta.topBreakerLabel)
        assertEquals(1, delta.topBreakerDelta)
    }

    @Test
    fun `delta stays empty without events`() {
        val delta = computeWeeklyDelta(
            currentEvents = emptyList(),
            previousEvents = emptyList(),
            currentSessionMinutes = 0,
            previousSessionMinutes = 0,
        )

        assertEquals(0, delta.unlocksDelta)
        assertEquals(0, delta.sessionMinutesDelta)
        assertNull(delta.topBreakerLabel)
        assertEquals(0, delta.topBreakerDelta)
    }

    @Test
    fun `top breaker delta handles missing previous events correctly`() {
        val currentEvents = listOf(
            focusEvent("b", "Game"),
            focusEvent("b", "Game"),
            focusEvent("b", "Game"),
        )
        val previousEvents = listOf(
            focusEvent("a", "Video"),
        )

        val delta = computeWeeklyDelta(
            currentEvents = currentEvents,
            previousEvents = previousEvents,
            currentSessionMinutes = 10,
            previousSessionMinutes = 5,
        )

        assertEquals(2, delta.unlocksDelta)
        assertEquals(5, delta.sessionMinutesDelta)
        assertEquals("Game", delta.topBreakerLabel)
        assertEquals(3, delta.topBreakerDelta)
    }

    private fun focusEvent(appKey: String, appLabel: String): FocusEventEntity {
        return FocusEventEntity(
            timestamp = 1L,
            appKey = appKey,
            appLabel = appLabel,
            reason = "",
            eventKind = "unlock",
            scheduleBlockLabel = null,
            unlockDurationMinutes = 5,
            usedEmergencyBypass = false,
            duringFocusSession = false,
            budgetBlocked = false,
            scheduleBlocked = false,
            effectiveDelaySeconds = 5,
        )
    }
}
