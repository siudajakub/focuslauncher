package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.FocusSessionDao
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lifecycle and active-session-projection coverage for [FocusSessionRepository] (issue #1).
 *
 * The repository is exercised against [InMemoryFocusSessionDao], a hand-written fake that
 * mirrors the SQL semantics of the real Room [FocusSessionDao] (latest-active lookup,
 * guarded conditional finish, replace-then-insert). This keeps the session lifecycle
 * contracts unit-testable on the JVM without an emulator or Room runtime.
 *
 * The framework-bound paths that this layer cannot reach on the JVM (WorkManager
 * scheduling/dispatch, NotificationManager DND mutation, DataStore-backed
 * focusSessionEndsAt persistence inside FocusPolicyService) are covered by the Pixel
 * smoke test, see docs/engineering/pixel-smoke-test.md.
 */
class FocusSessionRepositoryTest {

    /**
     * In-memory stand-in for the Room DAO. Replicates the contracts the repository relies on:
     *  - [insert] assigns an auto-increment id.
     *  - [getLatestActive] returns the most recently started session whose status matches.
     *  - [finishSessionIfActive] only updates when id + plannedEndsAt + Active status all match,
     *    returning the number of affected rows (1 or 0) just like the @Query.
     *  - [replaceActiveSession] is the real default method from the interface, so its
     *    replace-then-insert transaction logic is exercised as written in production.
     */
    private class InMemoryFocusSessionDao : FocusSessionDao {
        val sessions = mutableListOf<FocusSessionEntity>()
        private var nextId = 1L

        override suspend fun insert(session: FocusSessionEntity): Long {
            val id = nextId++
            sessions += session.copy(id = id)
            return id
        }

        override fun getSessionsSince(since: Long): Flow<List<FocusSessionEntity>> {
            return flowOf(sessions.filter { it.startedAt >= since }.sortedByDescending { it.startedAt })
        }

        override fun getRecent(limit: Int): Flow<List<FocusSessionEntity>> {
            return flowOf(sessions.sortedByDescending { it.startedAt }.take(limit))
        }

        override fun getLatest(): Flow<FocusSessionEntity?> {
            return flowOf(sessions.maxByOrNull { it.startedAt })
        }

        override suspend fun getLatestActive(status: String): FocusSessionEntity? {
            return sessions.filter { it.status == status }.maxByOrNull { it.startedAt }
        }

        override suspend fun finishSession(id: Long, endedAt: Long, status: String) {
            val index = sessions.indexOfFirst { it.id == id }
            if (index >= 0) {
                sessions[index] = sessions[index].copy(endedAt = endedAt, status = status)
            }
        }

        override suspend fun finishSessionIfActive(
            id: Long,
            expectedPlannedEndsAt: Long,
            endedAt: Long,
            activeStatus: String,
            finishedStatus: String,
        ): Int {
            val index = sessions.indexOfFirst {
                it.id == id &&
                    it.plannedEndsAt == expectedPlannedEndsAt &&
                    it.status == activeStatus
            }
            if (index < 0) return 0
            sessions[index] = sessions[index].copy(endedAt = endedAt, status = finishedStatus)
            return 1
        }
    }

    private fun repositoryWith(dao: InMemoryFocusSessionDao = InMemoryFocusSessionDao()) =
        dao to FocusSessionRepository(dao)

    // region session start

    @Test
    fun `starting a session produces the expected active-session projection`() = runBlocking {
        val (dao, repository) = repositoryWith()

        val session = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        assertTrue("session id should be assigned by the dao", session.id > 0L)
        assertEquals(FocusSessionStatus.Active.name, session.status)

        // The repository's view of the active session and the projection both agree on the deadline.
        val active = repository.getActiveSession()
        assertEquals(session.id, active?.id)
        assertEquals(5_000L, active?.plannedEndsAt)

        val projection = resolveFocusSessionReconciliation(
            activeSessionId = active?.id,
            activePlannedEndsAt = active?.plannedEndsAt,
            now = 1_000L,
        )
        assertEquals(
            FocusSessionReconciliation.Active(sessionId = session.id, plannedEndsAt = 5_000L),
            projection,
        )
        // The endsAt the launcher would persist matches the persisted session deadline.
        assertEquals(active?.plannedEndsAt, projection.projectedEndsAt)
        assertTrue(isFocusSessionActive(plannedEndsAt = active?.plannedEndsAt, now = 1_000L))
        assertEquals(1, dao.sessions.size)
    }

    @Test
    fun `starting a session replaces a previously active session`() = runBlocking {
        val (dao, repository) = repositoryWith()

        val first = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)
        val second = repository.startSession(startedAt = 2_000L, plannedEndsAt = 9_000L)

        // Only the newest session is Active; the prior one is Replaced (single active projection).
        val active = repository.getActiveSession()
        assertEquals(second.id, active?.id)
        assertEquals(9_000L, active?.plannedEndsAt)

        val replaced = dao.sessions.single { it.id == first.id }
        assertEquals(FocusSessionStatus.Replaced.name, replaced.status)
        assertEquals(2_000L, replaced.endedAt)
    }

    // endregion

    // region manual end

    @Test
    fun `manual end before the deadline ends the session early and clears the active projection`() =
        runBlocking {
            val (_, repository) = repositoryWith()
            repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

            val result = repository.endActiveSession(endedAt = 3_000L)

            result as FocusSessionEndResult.Finished
            assertEquals(FocusSessionStatus.EndedEarly, result.status)
            assertEquals(3_000L, result.session.endedAt)

            // Active projection is now empty.
            assertNull(repository.getActiveSession())
            assertEquals(
                FocusSessionReconciliation.NoActiveSession,
                resolveFocusSessionReconciliation(null, null, now = 3_000L),
            )
        }

    @Test
    fun `manual end at or after the deadline marks the session completed`() = runBlocking {
        val (_, repository) = repositoryWith()
        repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        val result = repository.endActiveSession(endedAt = 5_000L)

        result as FocusSessionEndResult.Finished
        assertEquals(FocusSessionStatus.Completed, result.status)
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `manual end with no active session is a no-op`() = runBlocking {
        val (_, repository) = repositoryWith()

        assertEquals(FocusSessionEndResult.NoActiveSession, repository.endActiveSession(endedAt = 3_000L))
    }

    @Test
    fun `manual end is idempotent and does not re-end an already ended session`() = runBlocking {
        val (dao, repository) = repositoryWith()
        repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        val first = repository.endActiveSession(endedAt = 3_000L)
        val second = repository.endActiveSession(endedAt = 4_000L)

        assertTrue(first is FocusSessionEndResult.Finished)
        // Second end finds no active session; the recorded endedAt is unchanged.
        assertEquals(FocusSessionEndResult.NoActiveSession, second)
        assertEquals(3_000L, dao.sessions.single().endedAt)
        assertEquals(FocusSessionStatus.EndedEarly.name, dao.sessions.single().status)
    }

    // endregion

    // region scheduled expiry

    @Test
    fun `scheduled expiry at the deadline completes the active session`() = runBlocking {
        val (_, repository) = repositoryWith()
        val session = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        val result = repository.finishExpectedSession(
            expectedSessionId = session.id,
            expectedPlannedEndsAt = session.plannedEndsAt,
            endedAt = 5_000L,
        )

        result as FocusSessionEndResult.Finished
        assertEquals(FocusSessionStatus.Completed, result.status)
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `scheduled expiry past the deadline completes the active session`() = runBlocking {
        val (_, repository) = repositoryWith()
        val session = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        val result = repository.finishExpectedSession(
            expectedSessionId = session.id,
            expectedPlannedEndsAt = session.plannedEndsAt,
            endedAt = 6_000L,
        )

        result as FocusSessionEndResult.Finished
        assertEquals(FocusSessionStatus.Completed, result.status)
        assertNull(repository.getActiveSession())
    }

    // endregion

    // region stale / duplicate worker

    @Test
    fun `worker for a superseded session is rejected as stale and does not touch the live session`() =
        runBlocking {
            val (dao, repository) = repositoryWith()
            val first = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)
            // A new session replaces the first; a late worker still carries the first session's id.
            val second = repository.startSession(startedAt = 2_000L, plannedEndsAt = 9_000L)

            val result = repository.finishExpectedSession(
                expectedSessionId = first.id,
                expectedPlannedEndsAt = first.plannedEndsAt,
                endedAt = 5_000L,
            )

            assertEquals(FocusSessionEndResult.StaleSession, result)
            // The live (second) session is untouched: still Active, no endedAt.
            val active = repository.getActiveSession()
            assertEquals(second.id, active?.id)
            assertNull(active?.endedAt)
            assertEquals(FocusSessionStatus.Active.name, active?.status)
            assertEquals(9_000L, active?.plannedEndsAt)
            // The replaced first session keeps the endedAt from the replace, not the stale worker.
            assertEquals(2_000L, dao.sessions.single { it.id == first.id }.endedAt)
        }

    @Test
    fun `duplicate worker run does not double-end or resurrect a session`() = runBlocking {
        val (dao, repository) = repositoryWith()
        val session = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        val first = repository.finishExpectedSession(
            expectedSessionId = session.id,
            expectedPlannedEndsAt = session.plannedEndsAt,
            endedAt = 5_000L,
        )
        // Same worker payload fires again (WorkManager re-delivery / retry).
        val second = repository.finishExpectedSession(
            expectedSessionId = session.id,
            expectedPlannedEndsAt = session.plannedEndsAt,
            endedAt = 7_000L,
        )

        assertTrue(first is FocusSessionEndResult.Finished)
        // The second run finds no active session and changes nothing.
        assertEquals(FocusSessionEndResult.NoActiveSession, second)
        val stored = dao.sessions.single()
        assertEquals(FocusSessionStatus.Completed.name, stored.status)
        assertEquals(5_000L, stored.endedAt)
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `worker with a mismatched deadline for the live session is rejected as stale`() = runBlocking {
        val (_, repository) = repositoryWith()
        val session = repository.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

        // Right id, wrong deadline (e.g. the session was extended): guard must reject it.
        val result = repository.finishExpectedSession(
            expectedSessionId = session.id,
            expectedPlannedEndsAt = 4_000L,
            endedAt = 5_000L,
        )

        assertEquals(FocusSessionEndResult.StaleSession, result)
        assertEquals(session.id, repository.getActiveSession()?.id)
    }

    // endregion

    // region restart recovery

    @Test
    fun `restart recovery rebuilds the same active-session projection from persisted data`() =
        runBlocking {
            // Simulate a process that started a session, then died (in-memory launcher state lost).
            val dao = InMemoryFocusSessionDao()
            val before = FocusSessionRepository(dao)
            val started = before.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

            // New process: fresh repository over the same persisted store, no carried-over state.
            val afterRestart = FocusSessionRepository(dao)
            val active = afterRestart.getActiveSession()
            val now = 2_000L
            val projection = resolveFocusSessionReconciliation(
                activeSessionId = active?.id,
                activePlannedEndsAt = active?.plannedEndsAt,
                now = now,
            )

            assertEquals(
                FocusSessionReconciliation.Active(sessionId = started.id, plannedEndsAt = 5_000L),
                projection,
            )
            // Reconstructed endsAt projection matches what was persisted at start.
            assertEquals(started.plannedEndsAt, projection.projectedEndsAt)
            assertTrue(isFocusSessionActive(active?.plannedEndsAt, now))
        }

    @Test
    fun `restart after the deadline reconciles to an expired projection and finishes the session`() =
        runBlocking {
            val dao = InMemoryFocusSessionDao()
            val before = FocusSessionRepository(dao)
            val started = before.startSession(startedAt = 1_000L, plannedEndsAt = 5_000L)

            // Restart happens after the planned deadline; reconciliation should classify Expired.
            val afterRestart = FocusSessionRepository(dao)
            val active = afterRestart.getActiveSession()
            val now = 6_000L
            val projection = resolveFocusSessionReconciliation(
                activeSessionId = active?.id,
                activePlannedEndsAt = active?.plannedEndsAt,
                now = now,
            )
            assertEquals(
                FocusSessionReconciliation.Expired(sessionId = started.id, plannedEndsAt = 5_000L),
                projection,
            )
            // Cleared projection: the launcher would persist endsAt = 0.
            assertEquals(0L, projection.projectedEndsAt)

            // Reconciliation then finishes the expired session, leaving no active session.
            val result = afterRestart.finishExpectedSession(
                expectedSessionId = (projection as FocusSessionReconciliation.Expired).sessionId,
                expectedPlannedEndsAt = projection.plannedEndsAt,
                endedAt = now,
            )
            result as FocusSessionEndResult.Finished
            assertEquals(FocusSessionStatus.Completed, result.status)
            assertNull(afterRestart.getActiveSession())
        }

    @Test
    fun `restart with no persisted session yields an empty projection`() = runBlocking {
        val repository = FocusSessionRepository(InMemoryFocusSessionDao())

        val active = repository.getActiveSession()
        assertNull(active)
        assertEquals(
            FocusSessionReconciliation.NoActiveSession,
            resolveFocusSessionReconciliation(active?.id, active?.plannedEndsAt, now = 1_000L),
        )
    }

    // endregion
}
