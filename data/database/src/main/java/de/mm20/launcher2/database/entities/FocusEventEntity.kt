package de.mm20.launcher2.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FocusEvent")
data class FocusEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val appKey: String,
    val appLabel: String,
    val reason: String,
    val unlockDurationMinutes: Int,
    val usedEmergencyBypass: Boolean,
    val duringFocusSession: Boolean,
    val budgetBlocked: Boolean,
    val scheduleBlocked: Boolean,
    val effectiveDelaySeconds: Int,
)
