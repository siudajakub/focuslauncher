package de.mm20.launcher2.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FocusSession")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val plannedEndsAt: Long,
    val endedAt: Long? = null,
    val status: String,
)
