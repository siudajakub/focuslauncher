package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_33_34 : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `FocusSession` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `plannedEndsAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `status` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
