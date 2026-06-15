package de.mm20.launcher2.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import de.mm20.launcher2.database.entities.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM FocusSession WHERE startedAt >= :since ORDER BY startedAt DESC")
    fun getSessionsSince(since: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM FocusSession ORDER BY startedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM FocusSession ORDER BY startedAt DESC LIMIT 1")
    fun getLatest(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM FocusSession WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestActive(status: String): FocusSessionEntity?

    @Query("UPDATE FocusSession SET endedAt = :endedAt, status = :status WHERE id = :id")
    suspend fun finishSession(id: Long, endedAt: Long, status: String)

    @Query(
        """
        UPDATE FocusSession
        SET endedAt = :endedAt, status = :finishedStatus
        WHERE id = :id
          AND plannedEndsAt = :expectedPlannedEndsAt
          AND status = :activeStatus
        """
    )
    suspend fun finishSessionIfActive(
        id: Long,
        expectedPlannedEndsAt: Long,
        endedAt: Long,
        activeStatus: String,
        finishedStatus: String,
    ): Int

    @Transaction
    suspend fun replaceActiveSession(
        session: FocusSessionEntity,
        activeStatus: String,
        replacedStatus: String,
    ): Long {
        getLatestActive(activeStatus)?.let { active ->
            finishSessionIfActive(
                id = active.id,
                expectedPlannedEndsAt = active.plannedEndsAt,
                endedAt = session.startedAt,
                activeStatus = activeStatus,
                finishedStatus = replacedStatus,
            )
        }
        return insert(session)
    }
}
