package de.mm20.launcher2.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.mm20.launcher2.database.migrations.Migration_35_36
import de.mm20.launcher2.database.migrations.Migration_36_37
import de.mm20.launcher2.database.migrations.Migration_37_38
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun deleteDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrate35To36_preservesTemporaryUnlockAndDeletesLegacyFocusData() {
        withTestDatabase { database ->
            createCustomAttributesV35(database)
            // Legacy FocusProfile payloads stored under the 'focus' type — must be dropped.
            insertCustomAttribute(database, "legacy-focus", "focus", "{\"focusMorningMode\":true}")
            insertCustomAttribute(database, "legacy-sleep", "focus", "{\"focusSleepMode\":true}")
            // FocusTemporaryUnlock payload — produced by FocusTemporaryUnlock.toDatabaseEntity:
            // type 'focus' with a 'temporary_unlock_until_millis' field — must be preserved.
            insertCustomAttribute(database, "temporary-unlock", "focus", "{\"temporary_unlock_until_millis\":12345}")
            // Non-focus attribute — out of scope of the migration, must be untouched.
            insertCustomAttribute(database, "unrelated", "tag", "{}")

            Migration_35_36().migrate(database)

            // (a) Legacy focus payloads removed.
            assertFalse(rowExists(database, "legacy-focus"))
            assertFalse(rowExists(database, "legacy-sleep"))
            // (b) FocusTemporaryUnlock preserved, with its value intact.
            assertTrue(rowExists(database, "temporary-unlock"))
            assertEquals(
                "{\"temporary_unlock_until_millis\":12345}",
                valueOf(database, "temporary-unlock"),
            )
            // Non-focus row untouched.
            assertTrue(rowExists(database, "unrelated"))

            database.query("SELECT `key` FROM CustomAttributes ORDER BY `key`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("temporary-unlock", cursor.getString(0))
                assertTrue(cursor.moveToNext())
                assertEquals("unrelated", cursor.getString(0))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    @Test
    fun migrate35To36_onlyDeletesFocusTypedRows() {
        // The migration is gated on `type = 'focus'`; a non-focus row must survive even when its
        // value coincidentally contains the temporary-unlock marker, and a focus row missing the
        // marker must be deleted regardless of any other fields it carries.
        withTestDatabase { database ->
            createCustomAttributesV35(database)
            insertCustomAttribute(database, "focus-no-marker", "focus", "{\"someOtherField\":1}")
            insertCustomAttribute(
                database,
                "tag-with-marker-substring",
                "tag",
                "{\"temporary_unlock_until_millis\":42}",
            )

            Migration_35_36().migrate(database)

            assertFalse(rowExists(database, "focus-no-marker"))
            assertTrue(rowExists(database, "tag-with-marker-substring"))
        }
    }

    @Test
    fun migrate36To37_dropsSearchActionTable() {
        withTestDatabase { database ->
            database.execSQL(
                """
                CREATE TABLE `SearchAction` (
                    `position` INTEGER NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            assertTrue(database.hasTable("SearchAction"))

            Migration_36_37().migrate(database)

            assertFalse(database.hasTable("SearchAction"))
        }
    }

    @Test
    fun migrate37To38_dropsRemovedFeatureTables() {
        withTestDatabase { database ->
            database.execSQL("CREATE TABLE `forecasts` (`timestamp` INTEGER NOT NULL PRIMARY KEY)")
            database.execSQL("CREATE TABLE `Currency` (`symbol` TEXT NOT NULL PRIMARY KEY)")
            database.execSQL("CREATE TABLE `Plugins` (`authority` TEXT NOT NULL PRIMARY KEY)")
            assertTrue(database.hasTable("forecasts"))
            assertTrue(database.hasTable("Currency"))
            assertTrue(database.hasTable("Plugins"))

            Migration_37_38().migrate(database)

            assertFalse(database.hasTable("forecasts"))
            assertFalse(database.hasTable("Currency"))
            assertFalse(database.hasTable("Plugins"))
        }
    }

    @Test
    fun migrate37To38_removesWeatherWidgetButKeepsOthers() {
        withTestDatabase { database ->
            createWidgetTableV37(database)
            insertWidget(database, id = 1, type = "weather", position = 0)
            insertWidget(database, id = 2, type = "music", position = 1)
            insertWidget(database, id = 3, type = "calendar", position = 2)

            Migration_37_38().migrate(database)

            database.query("SELECT `type` FROM Widget ORDER BY `position`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("music", cursor.getString(0))
                assertTrue(cursor.moveToNext())
                assertEquals("calendar", cursor.getString(0))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    @Test
    fun migrate37To38_toleratesTablesThatWereNeverCreated() {
        // A user who never had the removed features still has to migrate cleanly.
        withTestDatabase { database ->
            createWidgetTableV37(database)

            Migration_37_38().migrate(database)

            assertFalse(database.hasTable("forecasts"))
        }
    }

    @Test
    fun migrate37To38_addsFocusHistoryIndexes() {
        withTestDatabase { database ->
            createWidgetTableV37(database)
            database.execSQL(
                "CREATE TABLE `FocusEvent` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `timestamp` INTEGER NOT NULL, `appKey` TEXT NOT NULL)",
            )
            database.execSQL(
                "CREATE TABLE `FocusSession` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `startedAt` INTEGER NOT NULL, `status` TEXT NOT NULL)",
            )

            Migration_37_38().migrate(database)

            // Names must match what Room derives from the entities, or the identity check fails.
            assertTrue(database.hasIndex("index_FocusEvent_timestamp"))
            assertTrue(database.hasIndex("index_FocusEvent_appKey_timestamp"))
            assertTrue(database.hasIndex("index_FocusSession_startedAt"))
            assertTrue(database.hasIndex("index_FocusSession_status_startedAt"))
        }
    }

    @Test
    fun freshDatabase38_matchesCurrentRoomSchema() {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        try {
            val sqliteDatabase = database.openHelper.writableDatabase
            assertEquals(38, sqliteDatabase.version)
            assertTrue(sqliteDatabase.hasTable("FocusEvent"))
            assertTrue(sqliteDatabase.hasTable("FocusSession"))
            assertFalse(sqliteDatabase.hasTable("SearchAction"))
            assertFalse(sqliteDatabase.hasTable("forecasts"))
            assertFalse(sqliteDatabase.hasTable("Currency"))
            assertFalse(sqliteDatabase.hasTable("Plugins"))
        } finally {
            database.close()
        }
    }

    private fun withTestDatabase(block: (SupportSQLiteDatabase) -> Unit) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

        try {
            block(helper.writableDatabase)
        } finally {
            helper.close()
        }
    }

    private fun createCustomAttributesV35(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE `CustomAttributes` (
                `key` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                `id` INTEGER PRIMARY KEY AUTOINCREMENT
            )
            """.trimIndent(),
        )
    }

    private fun insertCustomAttribute(
        database: SupportSQLiteDatabase,
        key: String,
        type: String,
        value: String,
    ) {
        database.execSQL(
            "INSERT INTO CustomAttributes (`key`, `type`, `value`) VALUES (?, ?, ?)",
            arrayOf(key, type, value),
        )
    }

    private fun createWidgetTableV37(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE `Widget` (
                `id` BLOB NOT NULL PRIMARY KEY,
                `type` TEXT NOT NULL,
                `config` TEXT,
                `position` INTEGER NOT NULL,
                `parentId` BLOB
            )
            """.trimIndent(),
        )
    }

    private fun insertWidget(
        database: SupportSQLiteDatabase,
        id: Int,
        type: String,
        position: Int,
    ) {
        database.execSQL(
            "INSERT INTO Widget (`id`, `type`, `config`, `position`, `parentId`) VALUES (?, ?, NULL, ?, NULL)",
            arrayOf(byteArrayOf(id.toByte()), type, position),
        )
    }

    private fun rowExists(database: SupportSQLiteDatabase, key: String): Boolean {
        database.query("SELECT 1 FROM CustomAttributes WHERE `key` = ?", arrayOf(key)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun valueOf(database: SupportSQLiteDatabase, key: String): String {
        database.query("SELECT `value` FROM CustomAttributes WHERE `key` = ?", arrayOf(key)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            return cursor.getString(0)
        }
    }

    private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean {
        query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun SupportSQLiteDatabase.hasIndex(indexName: String): Boolean {
        query(
            "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf(indexName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
