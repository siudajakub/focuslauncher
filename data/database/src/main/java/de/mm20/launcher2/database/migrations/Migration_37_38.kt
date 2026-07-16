package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Drops the tables of the inherited weather, currency-converter and plugin features, which the
 * focus-first launcher no longer ships, and removes any weather widget the user still had on a
 * home screen since that widget type no longer deserializes.
 *
 * Also adds the focus-history indexes. The index names must match what Room derives from the
 * entities, or the schema-identity check fails on the next open.
 */
class Migration_37_38 : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `forecasts`")
        db.execSQL("DROP TABLE IF EXISTS `Currency`")
        db.execSQL("DROP TABLE IF EXISTS `Plugins`")
        db.execSQL("DELETE FROM `Widget` WHERE `type` = 'weather'")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_FocusEvent_timestamp` ON `FocusEvent` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_FocusEvent_appKey_timestamp` ON `FocusEvent` (`appKey`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_FocusSession_startedAt` ON `FocusSession` (`startedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_FocusSession_status_startedAt` ON `FocusSession` (`status`, `startedAt`)")
    }
}
