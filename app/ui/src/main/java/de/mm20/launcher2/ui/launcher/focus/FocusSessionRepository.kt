package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.database.AppDatabase
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

class FocusSessionRepository : KoinComponent {
    private val database: AppDatabase by inject()

    suspend fun startSession(startedAt: Long, plannedEndsAt: Long) {
        database.focusSessionDao().getLatestActive()?.let { active ->
            database.focusSessionDao().finishSession(
                id = active.id,
                endedAt = startedAt,
                status = FocusSessionStatus.Replaced.name,
            )
        }
        database.focusSessionDao().insert(
            FocusSessionEntity(
                startedAt = startedAt,
                plannedEndsAt = plannedEndsAt,
                status = FocusSessionStatus.Active.name,
            )
        )
    }

    suspend fun endActiveSession(endedAt: Long) {
        val active = database.focusSessionDao().getLatestActive() ?: return
        val status = if (endedAt >= active.plannedEndsAt) {
            FocusSessionStatus.Completed
        } else {
            FocusSessionStatus.EndedEarly
        }
        database.focusSessionDao().finishSession(active.id, endedAt, status.name)
    }

    fun getSessionsSince(since: Long): Flow<List<FocusSessionEntity>> {
        return database.focusSessionDao().getSessionsSince(since)
    }

    fun getRecentSessions(limit: Int = 20): Flow<List<FocusSessionEntity>> {
        return database.focusSessionDao().getRecent(limit)
    }

    fun getLatestSession(): Flow<FocusSessionEntity?> {
        return database.focusSessionDao().getLatest()
    }
}
