package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.FocusEventDao
import de.mm20.launcher2.database.FocusSessionDao
import de.mm20.launcher2.database.entities.FocusEventEntity
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Coverage for turn-away ([FocusEventKind.Resisted]) accounting and the accompanying
 * over-counting fixes in [FocusHistoryRepository]. Exercised against hand-written in-memory DAO
 * fakes (mirroring [FocusSessionRepositoryTest]) so the aggregation stays JVM-unit-testable.
 */
class FocusHistoryTurnAwayTest {

    private class InMemoryFocusEventDao : FocusEventDao {
        val events = mutableListOf<FocusEventEntity>()
        private var nextId = 1L

        override suspend fun insert(event: FocusEventEntity) {
            events += event.copy(id = nextId++)
        }

        override fun getEventsSince(since: Long): Flow<List<FocusEventEntity>> =
            flowOf(events.filter { it.timestamp >= since }.sortedByDescending { it.timestamp })

        override suspend fun getEventsSinceSuspend(since: Long): List<FocusEventEntity> =
            events.filter { it.timestamp >= since }.sortedByDescending { it.timestamp }

        override suspend fun getEventsForAppSince(appKey: String, since: Long): List<FocusEventEntity> =
            events.filter { it.appKey == appKey && it.timestamp >= since }.sortedByDescending { it.timestamp }

        override fun getRecent(limit: Int): Flow<List<FocusEventEntity>> =
            flowOf(events.sortedByDescending { it.timestamp }.take(limit))
    }

    private class EmptyFocusSessionDao : FocusSessionDao {
        override suspend fun insert(session: FocusSessionEntity): Long = 0L
        override fun getSessionsSince(since: Long): Flow<List<FocusSessionEntity>> = flowOf(emptyList())
        override fun getRecent(limit: Int): Flow<List<FocusSessionEntity>> = flowOf(emptyList())
        override fun getLatest(): Flow<FocusSessionEntity?> = flowOf(null)
        override suspend fun getLatestActive(status: String): FocusSessionEntity? = null
        override suspend fun finishSession(id: Long, endedAt: Long, status: String) {}
        override suspend fun finishSessionIfActive(
            id: Long,
            expectedPlannedEndsAt: Long,
            endedAt: Long,
            activeStatus: String,
            finishedStatus: String,
        ): Int = 0
    }

    private fun event(
        appKey: String = "app",
        appLabel: String = "App",
        kind: FocusEventKind,
        timestamp: Long,
        reason: String = "because",
        unlockDurationMinutes: Int = 5,
    ) = FocusEventEntity(
        timestamp = timestamp,
        appKey = appKey,
        appLabel = appLabel,
        reason = if (kind == FocusEventKind.Resisted) "" else reason,
        eventKind = kind.value,
        unlockDurationMinutes = if (kind == FocusEventKind.Resisted) 0 else unlockDurationMinutes,
        usedEmergencyBypass = false,
        duringFocusSession = false,
        budgetBlocked = false,
        scheduleBlocked = false,
        effectiveDelaySeconds = 10,
    )

    private fun repository(dao: InMemoryFocusEventDao) =
        FocusHistoryRepository(dao, EmptyFocusSessionDao())

    @Test
    fun `getResistedCountSince counts only turn-away events`() = runBlocking {
        val dao = InMemoryFocusEventDao()
        val repo = repository(dao)
        val now = System.currentTimeMillis()
        dao.insert(event(kind = FocusEventKind.Unlock, timestamp = now - 1_000))
        dao.insert(event(kind = FocusEventKind.Resisted, timestamp = now - 2_000))
        dao.insert(event(kind = FocusEventKind.Resisted, timestamp = now - 3_000))
        dao.insert(event(kind = FocusEventKind.ResumeAccepted, timestamp = now - 4_000))

        assertEquals(2, repo.getResistedCountSince(now - 10_000))
    }

    @Test
    fun `escalating friction ignores non-launch events`() = runBlocking {
        val dao = InMemoryFocusEventDao()
        val repo = repository(dao)
        val now = System.currentTimeMillis()
        dao.insert(event(kind = FocusEventKind.Unlock, timestamp = now - 1_000))
        dao.insert(event(kind = FocusEventKind.Resisted, timestamp = now - 2_000))
        dao.insert(event(kind = FocusEventKind.ResumeDismissed, timestamp = now - 3_000))
        dao.insert(event(kind = FocusEventKind.ResumeAccepted, timestamp = now - 4_000))

        // Only the single real Unlock counts as a recent launch.
        assertEquals(1, repo.getRecentAppLaunchTimestamps("app", now - 10_000).size)
    }

    @Test
    fun `weekly report excludes turn-aways from unlock metrics and reports them separately`() = runBlocking {
        val dao = InMemoryFocusEventDao()
        val repo = repository(dao)
        val now = System.currentTimeMillis()
        dao.insert(event(appLabel = "Video", kind = FocusEventKind.Unlock, timestamp = now - 1_000))
        dao.insert(event(appLabel = "Video", kind = FocusEventKind.Unlock, timestamp = now - 2_000))
        dao.insert(event(appLabel = "Chat", kind = FocusEventKind.Resisted, timestamp = now - 3_000))
        dao.insert(event(appLabel = "Chat", kind = FocusEventKind.Resisted, timestamp = now - 4_000))
        dao.insert(event(appLabel = "Chat", kind = FocusEventKind.Resisted, timestamp = now - 5_000))

        val report = repo.getWeeklyReport().first()

        assertEquals(2, report.totalUnlocks)
        assertEquals(3, report.resistedCount)
        // A turn-away must never read as a "focus breaker".
        assertEquals(listOf("Video" to 2), report.topFocusBreakers)
    }
}
