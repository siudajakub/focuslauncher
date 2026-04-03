package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_34_35 : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `FocusEvent` ADD COLUMN `eventKind` TEXT NOT NULL DEFAULT 'unlock'")
        db.execSQL("ALTER TABLE `FocusEvent` ADD COLUMN `scheduleBlockLabel` TEXT")
    }
}
