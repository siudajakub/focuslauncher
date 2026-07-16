package de.mm20.launcher2.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Every read is "since a timestamp, newest first", and the gate additionally scopes to one app.
// Without these the gate path full-scans and sorts the whole history on each distracting launch.
@Entity(
    tableName = "FocusEvent",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["appKey", "timestamp"]),
    ],
)
data class FocusEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val appKey: String,
    val appLabel: String,
    val reason: String,
    val eventKind: String = "unlock",
    val scheduleBlockLabel: String? = null,
    val unlockDurationMinutes: Int,
    val usedEmergencyBypass: Boolean,
    val duringFocusSession: Boolean,
    val budgetBlocked: Boolean,
    val scheduleBlocked: Boolean,
    val effectiveDelaySeconds: Int,
)
