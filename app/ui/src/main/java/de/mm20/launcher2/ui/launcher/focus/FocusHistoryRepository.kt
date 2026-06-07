package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.AppDatabase
import de.mm20.launcher2.database.entities.FocusEventEntity
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class FocusAttentionSignalKind {
    RecentUnlocks,
    SameAppDrift,
    MismatchUnlocks,
    AbandonedSessions,
    LauncherBounceBacks,
    MissedHabits,
    RepeatedBlockInterruptions,
    AlignedResumes,
}

data class FocusAttentionSignal(
    val kind: FocusAttentionSignalKind,
    val count: Int,
    val weight: Int,
)

data class FocusAttentionState(
    val appKey: String,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val recentUnlockCount: Int,
    val sameAppRepeatCount: Int,
    val mismatchUnlockCount: Int,
    val abandonedSessionCount: Int,
    val launcherBounceCount: Int,
    val missedHabitSignalCount: Int,
    val alignedResumeCount: Int,
    val repeatedBlockInterruptionCount: Int,
    val graceWindowActive: Boolean,
    val driftScore: Int,
    val signals: List<FocusAttentionSignal>,
) {
    companion object {
        fun idle(
            appKey: String,
            windowStartMillis: Long,
            windowEndMillis: Long,
        ): FocusAttentionState {
            return FocusAttentionState(
                appKey = appKey,
                windowStartMillis = windowStartMillis,
                windowEndMillis = windowEndMillis,
                recentUnlockCount = 0,
                sameAppRepeatCount = 0,
                mismatchUnlockCount = 0,
                abandonedSessionCount = 0,
                launcherBounceCount = 0,
                missedHabitSignalCount = 0,
                alignedResumeCount = 0,
                repeatedBlockInterruptionCount = 0,
                graceWindowActive = true,
                driftScore = 0,
                signals = emptyList(),
            )
        }
    }
}

data class FocusLogEvent(
    val appKey: String,
    val appLabel: String,
    val reason: String,
    val eventKind: String = FocusEventKind.Unlock.value,
    val scheduleBlockLabel: String? = null,
    val microStep: String? = null,
    val unlockDurationMinutes: Int,
    val usedEmergencyBypass: Boolean,
    val duringFocusSession: Boolean,
    val budgetBlocked: Boolean,
    val scheduleBlocked: Boolean,
    val effectiveDelaySeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class FocusEventKind(val value: String) {
    Unlock("unlock"),
    ResumeAccepted("resume_accepted"),
    ResumeDismissed("resume_dismissed"),
}

data class WeeklyFocusReport(
    val totalUnlocks: Int = 0,
    val totalUnlockMinutes: Int = 0,
    val averageDelaySeconds: Int = 0,
    val streakDays: Int = 0,
    val topFocusBreakers: List<Pair<String, Int>> = emptyList(),
    val topUnlockReasons: List<Pair<String, Int>> = emptyList(),
    val inSessionUnlocks: Int = 0,
    val unlocksPerDay: List<Pair<LocalDate, Int>> = emptyList(),
    val recentEvents: List<FocusEventEntity> = emptyList(),
    val totalSessions: Int = 0,
    val totalSessionMinutes: Int = 0,
    val sessionDays: Int = 0,
    val recentSessions: List<FocusSessionEntity> = emptyList(),
    val scheduledBlockUnlocks: Int = 0,
    val topInterruptedBlocks: List<Pair<String, Int>> = emptyList(),
    val recoveryAcceptedCount: Int = 0,
    val recoveryDismissedCount: Int = 0,
    val delta: WeeklyFocusDelta = WeeklyFocusDelta(),
)

data class WeeklyFocusDelta(
    val unlocksDelta: Int = 0,
    val sessionMinutesDelta: Int = 0,
    val topBreakerLabel: String? = null,
    val topBreakerDelta: Int = 0,
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
                eventKind = event.eventKind,
                scheduleBlockLabel = event.scheduleBlockLabel,
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

    suspend fun getRecentAppLaunchTimestamps(appKey: String, sinceMillis: Long): List<Long> {
        return getEventsForAppSince(appKey, sinceMillis).map { it.timestamp }
    }

    suspend fun getAttentionStateForApp(
        appKey: String,
        sinceMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): FocusAttentionState {
        val events = getEventsForAppSince(appKey, sinceMillis).sortedBy { it.timestamp }
        val sessions = database.focusSessionDao().getSessionsSince(sinceMillis).first()
        if (events.isEmpty() && sessions.isEmpty()) {
            return FocusAttentionState.idle(
                appKey = appKey,
                windowStartMillis = sinceMillis,
                windowEndMillis = nowMillis,
            )
        }

        val unlockEvents = events.filter { it.eventKind == FocusEventKind.Unlock.value }
        val recentUnlockCount = unlockEvents.size
        val sameAppRepeatCount = unlockEvents.zipWithNext().count { (previous, current) ->
            current.timestamp - previous.timestamp in 1..(2 * 60_000L)
        }
        val mismatchUnlockCount = unlockEvents.count { it.reason.isBlank() }
        val launcherBounceCount = unlockEvents.count { event ->
            event.unlockDurationMinutes in 1..5
        }
        val abandonedSessionCount = sessions.count { session ->
            session.status == FocusSessionStatus.EndedEarly.name ||
                session.status == FocusSessionStatus.Replaced.name
        }
        val missedHabitSignalCount = events.count {
            it.eventKind == FocusEventKind.ResumeDismissed.value
        }
        val alignedResumeCount = events.count {
            it.eventKind == FocusEventKind.ResumeAccepted.value
        }
        val repeatedBlockInterruptionCount = events.count {
            it.scheduleBlocked || it.budgetBlocked || it.usedEmergencyBypass
        }
        val signals = buildList {
            if (recentUnlockCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.RecentUnlocks, recentUnlockCount, 1))
            }
            if (sameAppRepeatCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.SameAppDrift, sameAppRepeatCount, 2))
            }
            if (mismatchUnlockCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.MismatchUnlocks, mismatchUnlockCount, 2))
            }
            if (abandonedSessionCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.AbandonedSessions, abandonedSessionCount, 3))
            }
            if (launcherBounceCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.LauncherBounceBacks, launcherBounceCount, 1))
            }
            if (missedHabitSignalCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.MissedHabits, missedHabitSignalCount, 2))
            }
            if (repeatedBlockInterruptionCount > 0) {
                add(
                    FocusAttentionSignal(
                        FocusAttentionSignalKind.RepeatedBlockInterruptions,
                        repeatedBlockInterruptionCount,
                        2,
                    )
                )
            }
            if (alignedResumeCount > 0) {
                add(FocusAttentionSignal(FocusAttentionSignalKind.AlignedResumes, alignedResumeCount, -2))
            }
        }
        val driftScore = signals.sumOf { signal -> signal.count * signal.weight }.coerceAtLeast(0)
        val graceWindowActive =
            recentUnlockCount <= 1 &&
                sameAppRepeatCount == 0 &&
                mismatchUnlockCount == 0 &&
                abandonedSessionCount == 0 &&
                launcherBounceCount == 0 &&
                missedHabitSignalCount == 0 &&
                repeatedBlockInterruptionCount == 0

        return FocusAttentionState(
            appKey = appKey,
            windowStartMillis = sinceMillis,
            windowEndMillis = nowMillis,
            recentUnlockCount = recentUnlockCount,
            sameAppRepeatCount = sameAppRepeatCount,
            mismatchUnlockCount = mismatchUnlockCount,
            abandonedSessionCount = abandonedSessionCount,
            launcherBounceCount = launcherBounceCount,
            missedHabitSignalCount = missedHabitSignalCount,
            alignedResumeCount = alignedResumeCount,
            repeatedBlockInterruptionCount = repeatedBlockInterruptionCount,
            graceWindowActive = graceWindowActive,
            driftScore = driftScore,
            signals = signals,
        )
    }

    fun getRecentEvents(limit: Int = 50): Flow<List<FocusEventEntity>> {
        return database.focusEventDao().getRecent(limit)
    }

    fun getWeeklyReport(): Flow<WeeklyFocusReport> {
        val now = System.currentTimeMillis()
        val weekMillis = ChronoUnit.DAYS.duration.multipliedBy(7).toMillis()
        val previousSince = now - weekMillis * 2
        val currentSince = now - weekMillis
        return combine(
            database.focusEventDao().getEventsSince(previousSince),
            database.focusSessionDao().getSessionsSince(previousSince),
        ) { events, sessions ->
            val zone = ZoneId.systemDefault()
            val currentEvents = events.filter { it.timestamp >= currentSince }
            val previousEvents = events.filter { it.timestamp in previousSince until currentSince }
            val currentSessions = sessions.filter { it.startedAt >= currentSince }
            val previousSessions = sessions.filter { it.startedAt in previousSince until currentSince }
            val groupedByDay = currentEvents.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            }.toSortedMap(compareByDescending { it })
            val breakers = currentEvents.groupingBy { it.appLabel }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            val reasons = currentEvents
                .mapNotNull { it.reason.trim().takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            val totalUnlockMinutes = currentEvents.sumOf { it.unlockDurationMinutes }
            val averageDelaySeconds = currentEvents
                .map { it.effectiveDelaySeconds }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toInt()
                ?: 0
            val normalizedCurrentSessions = currentSessions.map { session ->
                val effectiveEnd = session.endedAt ?: minOf(System.currentTimeMillis(), session.plannedEndsAt)
                session to ((effectiveEnd - session.startedAt).coerceAtLeast(0L) / 60_000L).toInt()
            }
            val normalizedPreviousSessions = previousSessions.map { session ->
                val effectiveEnd = session.endedAt ?: minOf(System.currentTimeMillis(), session.plannedEndsAt)
                session to ((effectiveEnd - session.startedAt).coerceAtLeast(0L) / 60_000L).toInt()
            }
            val scheduledUnlocks = currentEvents.filter {
                it.eventKind == FocusEventKind.Unlock.value && !it.scheduleBlockLabel.isNullOrBlank()
            }
            val interruptedBlocks = scheduledUnlocks
                .mapNotNull { it.scheduleBlockLabel?.takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            val delta = computeWeeklyDelta(
                currentEvents = currentEvents,
                previousEvents = previousEvents,
                currentSessionMinutes = normalizedCurrentSessions.sumOf { it.second },
                previousSessionMinutes = normalizedPreviousSessions.sumOf { it.second },
            )
            WeeklyFocusReport(
                totalUnlocks = currentEvents.size,
                totalUnlockMinutes = totalUnlockMinutes,
                averageDelaySeconds = averageDelaySeconds,
                streakDays = calculateStreak(currentEvents, zone),
                topFocusBreakers = breakers,
                topUnlockReasons = reasons,
                inSessionUnlocks = currentEvents.count { it.duringFocusSession },
                unlocksPerDay = groupedByDay.map { it.key to it.value.size },
                recentEvents = currentEvents.take(20),
                totalSessions = currentSessions.size,
                totalSessionMinutes = normalizedCurrentSessions.sumOf { it.second },
                sessionDays = currentSessions.map {
                    Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
                }.distinct().size,
                recentSessions = currentSessions.take(10),
                scheduledBlockUnlocks = scheduledUnlocks.size,
                topInterruptedBlocks = interruptedBlocks,
                recoveryAcceptedCount = currentEvents.count { it.eventKind == FocusEventKind.ResumeAccepted.value },
                recoveryDismissedCount = currentEvents.count { it.eventKind == FocusEventKind.ResumeDismissed.value },
                delta = delta,
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

internal fun computeWeeklyDelta(
    currentEvents: List<FocusEventEntity>,
    previousEvents: List<FocusEventEntity>,
    currentSessionMinutes: Int,
    previousSessionMinutes: Int,
): WeeklyFocusDelta {
    val currentBreakers = currentEvents.groupingBy { it.appLabel }.eachCount()
    val previousBreakers = previousEvents.groupingBy { it.appLabel }.eachCount()
    val topBreakerLabel = currentBreakers.maxByOrNull { it.value }?.key
    val topBreakerDelta = topBreakerLabel?.let { label ->
        (currentBreakers[label] ?: 0) - (previousBreakers[label] ?: 0)
    } ?: 0

    return WeeklyFocusDelta(
        unlocksDelta = currentEvents.size - previousEvents.size,
        sessionMinutesDelta = currentSessionMinutes - previousSessionMinutes,
        topBreakerLabel = topBreakerLabel,
        topBreakerDelta = topBreakerDelta,
    )
}
