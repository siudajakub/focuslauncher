package de.mm20.launcher2.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Reads are "since a start time, newest first" plus the active-session lookup by status.
@Entity(
    tableName = "FocusSession",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["status", "startedAt"]),
    ],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val plannedEndsAt: Long,
    val endedAt: Long? = null,
    val status: String,
)
