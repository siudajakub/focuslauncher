package de.mm20.launcher2.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.mm20.launcher2.database.entities.FocusEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusEventDao {
    @Insert
    suspend fun insert(event: FocusEventEntity)

    @Query("SELECT * FROM FocusEvent WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<FocusEventEntity>>

    @Query("SELECT * FROM FocusEvent WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSinceSuspend(since: Long): List<FocusEventEntity>

    @Query("SELECT * FROM FocusEvent WHERE appKey = :appKey AND timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsForAppSince(appKey: String, since: Long): List<FocusEventEntity>

    @Query("SELECT * FROM FocusEvent ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<FocusEventEntity>>
}
