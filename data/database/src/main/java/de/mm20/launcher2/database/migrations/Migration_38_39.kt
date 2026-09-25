package de.mm20.launcher2.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_38_39 : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_FocusEvent_appKey_timestamp`")
        db.execSQL("DROP INDEX IF EXISTS `index_FocusEvent_timestamp`")
        db.execSQL("DROP INDEX IF EXISTS `index_FocusSession_startedAt`")
        db.execSQL("DROP INDEX IF EXISTS `index_FocusSession_status_startedAt`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `forecasts` (
                `timestamp` INTEGER NOT NULL,
                `temperature` REAL NOT NULL,
                `minTemp` REAL NOT NULL,
                `maxTemp` REAL NOT NULL,
                `pressure` REAL NOT NULL,
                `humidity` REAL NOT NULL,
                `icon` INTEGER NOT NULL,
                `condition` TEXT NOT NULL,
                `clouds` INTEGER NOT NULL,
                `windSpeed` REAL NOT NULL,
                `windDirection` REAL NOT NULL,
                `rain` REAL NOT NULL,
                `snow` REAL NOT NULL,
                `night` INTEGER NOT NULL,
                `location` TEXT NOT NULL,
                `provider` TEXT NOT NULL,
                `providerUrl` TEXT NOT NULL,
                `rainProbability` INTEGER NOT NULL,
                `snowProbability` INTEGER NOT NULL,
                `uvIndex` REAL NOT NULL,
                `updateTime` INTEGER NOT NULL,
                PRIMARY KEY(`timestamp`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Currency` (
                `symbol` TEXT NOT NULL,
                `value` REAL NOT NULL,
                `lastUpdate` INTEGER NOT NULL,
                PRIMARY KEY(`symbol`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Plugins` (
                `authority` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT,
                `packageName` TEXT NOT NULL,
                `className` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `settingsActivity` TEXT,
                `enabled` INTEGER NOT NULL,
                PRIMARY KEY(`authority`)
            )
            """.trimIndent(),
        )
    }
}
