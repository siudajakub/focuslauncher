package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_32_33 : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `FocusEvent` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `appKey` TEXT NOT NULL,
                `appLabel` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `unlockDurationMinutes` INTEGER NOT NULL,
                `usedEmergencyBypass` INTEGER NOT NULL,
                `duringFocusSession` INTEGER NOT NULL,
                `budgetBlocked` INTEGER NOT NULL,
                `scheduleBlocked` INTEGER NOT NULL,
                `effectiveDelaySeconds` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
