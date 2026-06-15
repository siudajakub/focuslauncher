---
name: room-migration
description: Implement and verify Room database changes in FocusLauncher. Use when adding, removing, or changing entities, columns, indices, DAOs, focus history/session persistence, database versions, migrations, or exported schemas.
---

# Room Migration

1. Inspect `data/database/.../AppDatabase.kt`, the affected entity and DAO, existing migrations, exported schemas, and `AppDatabaseMigrationTest`.
2. Define the old and new schema behavior, including data preservation and deletion rules.
3. Change entity, DAO, database version, and migration together. Register the migration in the database builder.
4. Avoid destructive migration unless the user explicitly accepts data loss.
5. Add or update migration tests that create the previous schema, insert representative data, migrate, and assert both schema and retained data.
6. Update the exported Room schema for the new version.
7. Run JVM tests that cover pure logic, then `./gradlew :data:database:connectedDebugAndroidTest` with an emulator or device.
8. Run `./gradlew :app:app:assembleDefaultDebug` to catch cross-module integration errors.
9. Report exact commands and results. If instrumentation cannot run, state that migration behavior remains unverified.

For focus migrations, preserve valid `FocusTemporaryUnlock` payloads and verify cleanup predicates against realistic serialized values.
