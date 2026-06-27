# Project Status

Last reviewed: 2026-06-28
Branch reviewed: `stabilize/focus-launcher`
Status: active stabilization; the latest stabilization and time-awareness work is committed (`2c96119b`) and the working tree is clean.

## Current Product State

- The launcher is being reduced from general-purpose Kvaesitso toward an apps-first, focus-first product.
- Focus app classification uses global essential and distracting key sets.
- Focus sessions, temporary unlocks, launch friction, daily limits, focus history, and weekly focus insights exist in the current tree.
- Time awareness is wired end to end: a Time Awareness settings section (enable toggle, interval, usage-access and notification permission prompts) with `TimeBlindnessService` and `TimeBlindnessReceiver` that start on app launch and on toggle, on a safe foreground-service path for API 34+.
- Quick Capture is lossless: notes persist and list locally, with optional share.
- Search surfaces matching pinned shortcuts; browser/PWA "add to home screen" shortcuts are tagged with a (web) label.
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

Documentation and build verification refreshed on 2026-06-28 with JDK 21 on a clean `stabilize/focus-launcher` tree.

- `python3 tools/check_agent_docs.py`: passed; it now also validates the session scripts and the `SessionStart`/`PreCompact` hook wiring.
- `python3 -m py_compile tools/check_agent_docs.py`: passed.
- `./gradlew :app:ui:testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL (418 tasks executed); focus unit tests pass on the current tree.
- `./gradlew :app:app:assembleDefaultDebug`: BUILD SUCCESSFUL; `app/app/build/outputs/apk/default/debug/app-default-debug.apk` is produced.
- Stabilization commit `2c96119b` recorded `:app:app:compileDefaultDebugKotlin` BUILD SUCCESSFUL and passing focus unit tests when it landed (off-main gate evaluation, weekly-report `flowOn`, hoisted DataStore reads, focus-streak fix, API 34+ time-awareness service path).
- Pixel smoke test: not re-run in this documentation wave; the last recorded run was 2026-06-14.
- `:data:database:connectedDebugAndroidTest`: not run locally (needs an Android emulator); covered by CI.

The working tree is clean and the stabilization work is committed, but the branch is not yet merged to `main` and has not had a full release verification, so this snapshot does not mean the branch is release-ready.

## Work Tracking

Product and cleanup work is tracked in GitHub Issues and the `FocusLauncher Stabilization` project board:

- Project: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

This file records verified state only; it is not a backlog.
