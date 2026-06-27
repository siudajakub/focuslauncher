package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.entities.FocusEventEntity
import de.mm20.launcher2.database.entities.FocusSessionEntity
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
    fun `focus streak is zero without sessions`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)

        val streak = calculateFocusStreakDays(
            sessions = emptyList(),
            zone = zone,
            today = today,
        )

        assertEquals(0, streak)
    }

    @Test
    fun `focus streak ignores future sessions and stays zero`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val futureSession = focusSession(today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())

        val streak = calculateFocusStreakDays(
            sessions = listOf(futureSession),
            zone = zone,
            today = today,
        )

        assertEquals(0, streak)
    }

    @Test
    fun `focus streak counts a session today as one`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val todaySession = focusSession(today.atStartOfDay(zone).toInstant().toEpochMilli())

        val streak = calculateFocusStreakDays(
            sessions = listOf(todaySession),
            zone = zone,
            today = today,
        )

        assertEquals(1, streak)
    }

    @Test
    fun `focus streak counts consecutive focus days backward from today`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val sessions = (0..2).map { offset ->
            focusSession(today.minusDays(offset.toLong()).atStartOfDay(zone).toInstant().toEpochMilli())
        }

        val streak = calculateFocusStreakDays(
            sessions = sessions,
            zone = zone,
            today = today,
        )

        assertEquals(3, streak)
    }

    @Test
    fun `focus streak grace rule starts from yesterday when today is missing`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val sessions = listOf(
            focusSession(today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            focusSession(today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()),
        )

        val streak = calculateFocusStreakDays(
            sessions = sessions,
            zone = zone,
            today = today,
        )

        assertEquals(2, streak)
    }

    @Test
    fun `focus streak stops at the first gap`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val sessions = listOf(
            focusSession(today.atStartOfDay(zone).toInstant().toEpochMilli()),
            // gap on day-1
            focusSession(today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()),
        )

        val streak = calculateFocusStreakDays(
            sessions = sessions,
            zone = zone,
            today = today,
        )

        assertEquals(1, streak)
    }

    @Test
    fun `focus streak is zero when last focus day is older than yesterday`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 6, 14)
        val staleSession = focusSession(today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli())

        val streak = calculateFocusStreakDays(
            sessions = listOf(staleSession),
            zone = zone,
            today = today,
        )

        assertEquals(0, streak)
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

    private fun focusSession(
        startedAt: Long,
        status: String = FocusSessionStatus.Completed.name,
    ): FocusSessionEntity {
        return FocusSessionEntity(
            startedAt = startedAt,
            plannedEndsAt = startedAt + 25 * 60_000L,
            endedAt = startedAt + 25 * 60_000L,
            status = status,
        )
    }
}
