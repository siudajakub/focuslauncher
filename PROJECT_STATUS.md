# Project Status

Last reviewed: 2026-06-15
Branch reviewed: `stabilize/focus-launcher`
Status: active stabilization; the current worktree contains substantial uncommitted product changes.

## Current Product State

- The launcher is being reduced from general-purpose Kvaesitso toward an apps-first, focus-first product.
- Focus app classification uses global essential and distracting key sets.
- Focus sessions, temporary unlocks, launch friction, daily limits, time-awareness support, and focus reporting exist in the current tree.
- The settings menu is now organized into a two-item hub: Focus Settings and Launcher Settings, replacing the previous monolithic structure.
- Calculator, website search, Wikipedia, Nextcloud, and Owncloud modules are removed from the active Gradle graph in the current tree.
- Weather, plugins, feed, music, unit conversion, widgets, calendar, contacts, files, and locations still exist technically. Some are hidden or reduced in product UI, but they are not physically removed.
- Architectural note: Focus system data and services (e.g., FocusPolicyService, FocusSessionRepository) currently reside in app/ui, violating strict module boundaries.

## Architecture Snapshot

- `FocusProfile` is no longer an active model. Legacy focus attributes are cleaned by database migration `35 -> 36`.
- App classification source of truth: `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Temporary access source of truth: `FocusTemporaryUnlock` in custom attributes.
- Session lifecycle: `FocusSessionRepository`, `FocusSessionRuntime`, expiry scheduling, and `FocusPolicyService`.
- Launch policy is coordinated through `FocusLaunchCoordinator` and `FocusPolicyService`.

## Verification Snapshot

Documentation audit completed on 2026-06-15.

- `python3 tools/check_agent_docs.py`: passed.
- `python3 -m py_compile tools/check_agent_docs.py tools/create_github_backlog.py`: passed.
- `./gradlew :app:ui:testDebugUnitTest --tests de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSupportTest --stacktrace`: passed with JDK 21.
- `./gradlew :data:database:compileDebugAndroidTestKotlin :app:ui:testDebugUnitTest --tests de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSupportTest --stacktrace`: passed with JDK 21.
- `./gradlew test :app:app:assembleDefaultDebug --stacktrace`: passed with JDK 21.
- `./gradlew :app:ui:testDebugUnitTest`: passed with JDK 21 after Focus System settings regrouping and focus report streak regression coverage.
- `./gradlew :app:app:assembleDefaultDebug`: passed with JDK 21 after Focus System settings regrouping.
- Pixel 8 smoke test on 2026-06-14: installed `app-default-debug.apk`, set `de.mm20.launcher2.debug` as the default HOME package via ADB, verified `LauncherActivity` handles HOME, opened Focus System settings and the Basics, Friction & Delays, and Time Management subcategory screens without new crash or ANR entries.
- `:data:database:connectedDebugAndroidTest`: not run locally in this wave; it is covered by CI because it needs an Android emulator.
- `python3 tools/create_github_backlog.py`: passed after GitHub CLI authentication and project scope refresh.

The worktree still contains substantial uncommitted product changes, so this snapshot verifies the current local tree but does not mean the branch is release-ready.

## Work Tracking

Product and cleanup work is tracked in GitHub Issues and the `FocusLauncher Stabilization` project board:

- Project: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

This file records verified state only; it is not a backlog.
