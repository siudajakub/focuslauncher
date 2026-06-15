package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.entities.FocusEventEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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

    @Test
    fun `focus streak ignores future events instead of walking forever`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val futureEvent = focusEvent(
            appLabel = "Video",
            timestamp = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val streak = calculateFocusStreakDays(
            events = listOf(futureEvent),
            zone = zone,
            today = today,
        )

        assertEquals(0, streak)
    }

    @Test
    fun `focus streak reports days since latest current week event`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val twoDaysAgoEvent = focusEvent(
            appLabel = "Video",
            timestamp = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val streak = calculateFocusStreakDays(
            events = listOf(twoDaysAgoEvent),
            zone = zone,
            today = today,
        )

        assertEquals(2, streak)
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
