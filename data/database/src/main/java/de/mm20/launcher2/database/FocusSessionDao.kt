package de.mm20.launcher2.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Query("SELECT * FROM FocusSession WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestActive(): FocusSessionEntity?

    @Query("UPDATE FocusSession SET endedAt = :endedAt, status = :status WHERE id = :id")
    suspend fun finishSession(id: Long, endedAt: Long, status: String)
}
