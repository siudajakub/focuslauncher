package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.entities.FocusEventEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusHistoryRepositoryModelsTest {
    @Test
    fun `weekly delta compares current and previous windows`() {
        val currentEvents = listOf(
            focusEvent(appLabel = "Video"),
            focusEvent(appLabel = "Video"),
            focusEvent(appLabel = "Chat"),
        )
        val previousEvents = listOf(
            focusEvent(appLabel = "Video"),
            focusEvent(appLabel = "Mail"),
        )

        val delta = computeWeeklyDelta(
            currentEvents = currentEvents,
            previousEvents = previousEvents,
            currentSessionMinutes = 90,
            previousSessionMinutes = 60,
        )

        assertEquals(1, delta.unlocksDelta)
        assertEquals(30, delta.sessionMinutesDelta)
        assertEquals("Video", delta.topBreakerLabel)
        assertEquals(1, delta.topBreakerDelta)
    }

    private fun focusEvent(
        appLabel: String,
        timestamp: Long = 0L,
    ): FocusEventEntity {
        return FocusEventEntity(
            timestamp = timestamp,
            appKey = appLabel.lowercase(),
            appLabel = appLabel,
            reason = "",
            unlockDurationMinutes = 1,
            usedEmergencyBypass = false,
            duringFocusSession = false,
            budgetBlocked = false,
            scheduleBlocked = false,
            effectiveDelaySeconds = 0,
        )
    }
}
