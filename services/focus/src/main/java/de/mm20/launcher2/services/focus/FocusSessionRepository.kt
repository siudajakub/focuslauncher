package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.AppDatabase
import de.mm20.launcher2.database.FocusSessionDao
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

enum class FocusSessionStatus {
    Active,
    Completed,
    EndedEarly,
    Replaced,
}

data class FocusSessionSummary(
    val session: FocusSessionEntity,
    val durationMinutes: Int,
    val unlockCount: Int,
)

sealed interface FocusSessionEndResult {
    data class Finished(
        val session: FocusSessionEntity,
        val status: FocusSessionStatus,
    ) : FocusSessionEndResult

    data object NoActiveSession : FocusSessionEndResult
    data object StaleSession : FocusSessionEndResult
}

class FocusSessionRepository internal constructor(
    private val dao: FocusSessionDao,
) {

    constructor(database: AppDatabase) : this(database.focusSessionDao())

    suspend fun startSession(startedAt: Long, plannedEndsAt: Long): FocusSessionEntity {
        val session = FocusSessionEntity(
            startedAt = startedAt,
            plannedEndsAt = plannedEndsAt,
            status = FocusSessionStatus.Active.name,
        )
        val id = dao.replaceActiveSession(
            session = session,
            activeStatus = FocusSessionStatus.Active.name,
            replacedStatus = FocusSessionStatus.Replaced.name,
        )
        return session.copy(id = id)
    }

    suspend fun getActiveSession(): FocusSessionEntity? {
        return dao.getLatestActive(FocusSessionStatus.Active.name)
    }

    suspend fun endActiveSession(endedAt: Long): FocusSessionEndResult {
        val active = getActiveSession() ?: return FocusSessionEndResult.NoActiveSession
        return finishExpectedSession(
            expectedSessionId = active.id,
            expectedPlannedEndsAt = active.plannedEndsAt,
            endedAt = endedAt,
        )
    }

    suspend fun finishExpectedSession(
        expectedSessionId: Long,
        expectedPlannedEndsAt: Long,
        endedAt: Long,
    ): FocusSessionEndResult {
        val active = getActiveSession() ?: return FocusSessionEndResult.NoActiveSession
        if (!isExpectedFocusSession(
                activeSessionId = active.id,
                activePlannedEndsAt = active.plannedEndsAt,
                expectedSessionId = expectedSessionId,
                expectedPlannedEndsAt = expectedPlannedEndsAt,
            )
        ) {
            return FocusSessionEndResult.StaleSession
        }
        val status = if (endedAt >= active.plannedEndsAt) {
            FocusSessionStatus.Completed
        } else {
            FocusSessionStatus.EndedEarly
        }
        val updatedRows = dao.finishSessionIfActive(
            id = active.id,
            expectedPlannedEndsAt = active.plannedEndsAt,
            endedAt = endedAt,
            activeStatus = FocusSessionStatus.Active.name,
            finishedStatus = status.name,
        )
        return if (updatedRows == 1) {
            FocusSessionEndResult.Finished(
                session = active.copy(endedAt = endedAt, status = status.name),
                status = status,
            )
        } else {
            FocusSessionEndResult.StaleSession
        }
    }

    fun getSessionsSince(since: Long): Flow<List<FocusSessionEntity>> {
        return dao.getSessionsSince(since)
    }

    fun getRecentSessions(limit: Int = 20): Flow<List<FocusSessionEntity>> {
        return dao.getRecent(limit)
    }

    fun getLatestSession(): Flow<FocusSessionEntity?> {
        return dao.getLatest()
    }
}
