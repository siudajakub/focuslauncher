package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.AppDatabase
import de.mm20.launcher2.database.entities.FocusEventEntity
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class FocusLogEvent(
    val appKey: String,
    val appLabel: String,
    val reason: String,
    val unlockDurationMinutes: Int,
    val usedEmergencyBypass: Boolean,
    val duringFocusSession: Boolean,
    val budgetBlocked: Boolean,
    val scheduleBlocked: Boolean,
    val effectiveDelaySeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

data class WeeklyFocusReport(
    val totalUnlocks: Int = 0,
    val totalUnlockMinutes: Int = 0,
    val averageDelaySeconds: Int = 0,
    val streakDays: Int = 0,
    val topFocusBreakers: List<Pair<String, Int>> = emptyList(),
    val topUnlockReasons: List<Pair<String, Int>> = emptyList(),
    val bypassUnlocks: Int = 0,
    val inSessionUnlocks: Int = 0,
    val unlocksPerDay: List<Pair<LocalDate, Int>> = emptyList(),
    val recentEvents: List<FocusEventEntity> = emptyList(),
    val totalSessions: Int = 0,
    val totalSessionMinutes: Int = 0,
    val sessionDays: Int = 0,
    val recentSessions: List<FocusSessionEntity> = emptyList(),
)

class FocusHistoryRepository : KoinComponent {
    private val database: AppDatabase by inject()

    suspend fun logEvent(event: FocusLogEvent) {
        database.focusEventDao().insert(
            FocusEventEntity(
                timestamp = event.timestamp,
                appKey = event.appKey,
                appLabel = event.appLabel,
                reason = event.reason,
                unlockDurationMinutes = event.unlockDurationMinutes,
                usedEmergencyBypass = event.usedEmergencyBypass,
                duringFocusSession = event.duringFocusSession,
                budgetBlocked = event.budgetBlocked,
                scheduleBlocked = event.scheduleBlocked,
                effectiveDelaySeconds = event.effectiveDelaySeconds,
            )
        )
    }

    suspend fun getEventsForAppSince(appKey: String, since: Long): List<FocusEventEntity> {
        return database.focusEventDao().getEventsForAppSince(appKey, since)
    }

    fun getRecentEvents(limit: Int = 50): Flow<List<FocusEventEntity>> {
        return database.focusEventDao().getRecent(limit)
    }

    fun getWeeklyReport(): Flow<WeeklyFocusReport> {
        val since = System.currentTimeMillis() - ChronoUnit.DAYS.duration.multipliedBy(7).toMillis()
        return combine(
            database.focusEventDao().getEventsSince(since),
            database.focusSessionDao().getSessionsSince(since),
        ) { events, sessions ->
            val zone = ZoneId.systemDefault()
            val groupedByDay = events.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            }.toSortedMap(compareByDescending { it })
            val breakers = events.groupingBy { it.appLabel }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            val reasons = events
                .mapNotNull { it.reason.trim().takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            val totalUnlockMinutes = events.sumOf { it.unlockDurationMinutes }
            val averageDelaySeconds = events
                .map { it.effectiveDelaySeconds }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toInt()
                ?: 0
            val normalizedSessions = sessions.map { session ->
                val effectiveEnd = session.endedAt ?: minOf(System.currentTimeMillis(), session.plannedEndsAt)
                session to ((effectiveEnd - session.startedAt).coerceAtLeast(0L) / 60_000L).toInt()
            }
            WeeklyFocusReport(
                totalUnlocks = events.size,
                totalUnlockMinutes = totalUnlockMinutes,
                averageDelaySeconds = averageDelaySeconds,
                streakDays = calculateStreak(events, zone),
                topFocusBreakers = breakers,
                topUnlockReasons = reasons,
                bypassUnlocks = events.count { it.usedEmergencyBypass },
                inSessionUnlocks = events.count { it.duringFocusSession },
                unlocksPerDay = groupedByDay.map { it.key to it.value.size },
                recentEvents = events.take(20),
                totalSessions = sessions.size,
                totalSessionMinutes = normalizedSessions.sumOf { it.second },
                sessionDays = sessions.map {
                    Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
                }.distinct().size,
                recentSessions = sessions.take(10),
            )
        }
    }

    private fun calculateStreak(events: List<FocusEventEntity>, zone: ZoneId): Int {
        val launchDays = events.map {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }.toSet()
        var streak = 0
        var currentDay = LocalDate.now(zone)
        while (!launchDays.contains(currentDay)) {
            streak++
            currentDay = currentDay.minusDays(1)
        }
        return streak
    }
}
