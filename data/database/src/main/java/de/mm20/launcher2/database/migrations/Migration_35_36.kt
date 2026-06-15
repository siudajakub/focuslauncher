package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_35_36 : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove legacy focus attributes (like focusMorningMode, focusSleepMode) that were part of FocusProfile
        // FocusTemporaryUnlock uses the 'focus' type but contains 'temporary_unlock_until_millis'
        db.execSQL("DELETE FROM CustomAttributes WHERE type = 'focus' AND value NOT LIKE '%temporary_unlock_until_millis%'")
    }
}
