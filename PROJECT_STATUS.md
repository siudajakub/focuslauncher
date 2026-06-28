# Project Status

Last reviewed: 2026-06-28
Branch reviewed: `stabilize/focus-launcher`
Status: active stabilization. A backlog-clearing wave is committed on `stabilize/focus-launcher` (Koin DI for `:services:focus`, `FocusReviewModels` extraction, focus session-lifecycle tests, migration-test hardening, dead search-preference removal, focus-copy i18n, and four new engineering docs). Not yet pushed or merged to `main`.

## Current Product State

- The launcher is being reduced from general-purpose Kvaesitso toward an apps-first, focus-first product.
- Focus app classification uses global essential and distracting key sets.
- Focus sessions, temporary unlocks, launch friction, daily limits, focus history, and weekly focus insights exist in the current tree.
- Time awareness is wired end to end: a Time Awareness settings section (enable toggle, interval, usage-access and notification permission prompts) with `TimeBlindnessService` and `TimeBlindnessReceiver` that start on app launch and on toggle, on a safe foreground-service path for API 34+.
- Quick Capture is lossless: notes persist and list locally, with optional share.
- Search surfaces matching pinned shortcuts; browser/PWA "add to home screen" shortcuts are tagged with a (web) label.
- The settings menu is now organized into a two-item hub: Focus Settings and Launcher Settings, replacing the previous monolithic structure.
- Calculator, website search, Wikipedia, Nextcloud, and Owncloud modules are removed from the active Gradle graph in the current tree. Their dead preference wrappers (`CalculatorSearchSettings`, `WebsiteSearchSettings`, `WikipediaSearchSettings`) and five orphaned persisted fields are now also removed; the DataStore serializer's `ignoreUnknownKeys = true` makes this safe for existing installs.
- Nextcloud/Owncloud still back live WebDAV file search (`libs/webdav`) and account integration (`services/accounts`); these are retained pending the integrations decision (#4).
- Weather, plugins, feed, music, unit conversion, widgets, calendar, contacts, files, and locations still exist technically. Some are hidden or reduced in product UI, but they are not physically removed. Proposed classifications are drafted in `docs/engineering/integrations-decision.md` (pending owner sign-off).
- Focus data and services — classification, policy, sessions, history, session runtime, and the session-expiry worker — live in the `:services:focus` module and are now wired through a Koin `focusModule` with constructor injection (no `KoinComponent` self-injection). `FocusReviewModels` has been moved into `:services:focus` (decoupled from `app/ui`'s `R` via `core/i18n`). `FocusLaunchCoordinator` still resides in `app/ui`: its move is blocked by a circular dependency on the `app/ui` `FocusGateActivity` and needs a gate-launcher interface inversion (follow-up on #49).

## Architecture Snapshot

- `FocusProfile` is no longer an active model. Legacy focus attributes are cleaned by database migration `35 -> 36`.
- App classification source of truth: `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Temporary access source of truth: `FocusTemporaryUnlock` in custom attributes.
- Session lifecycle (`:services:focus`): `FocusSessionRepository`, `FocusSessionRuntime`, `FocusSessionExpiryWorker` scheduling, and `FocusPolicyService`.
- Launch policy: `FocusPolicyService` (`:services:focus`), coordinated from `FocusLaunchCoordinator` (still in `app/ui`).

## Verification Snapshot

Refreshed on 2026-06-28 with JDK 21 on `stabilize/focus-launcher`, after the backlog-clearing wave (Koin DI, `FocusReviewModels` move, focus lifecycle tests, migration hardening, dead-preference removal, focus-copy i18n).

- `python3 tools/check_agent_docs.py`: passed; it also validates the session scripts and the `SessionStart`/`PreCompact` hook wiring.
- `./gradlew test :app:app:assembleDefaultDebug :data:database:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. All module unit tests pass, `app/app/build/outputs/apk/default/debug/app-default-debug.apk` is produced, and the instrumented DB migration tests compile.
- `./gradlew :services:focus:testDebugUnitTest`: BUILD SUCCESSFUL; 73 focus tests pass (59 prior + 14 new session-lifecycle tests covering start, manual end, scheduled expiry, idempotent stale-worker runs, and restart recovery).
- Pixel smoke test: not re-run in this wave; the last recorded run was 2026-06-14. The step-by-step checklist now lives in `docs/engineering/pixel-smoke-test.md`.
- `:data:database:connectedDebugAndroidTest`: not run locally (needs an Android emulator); the hardened `Migration_35_36`/`Migration_36_37` tests compile and are covered by CI.

The working tree is clean and the stabilization work is committed, but the branch is not yet merged to `main` and has not had a full release verification, so this snapshot does not mean the branch is release-ready.

## Work Tracking

Product and cleanup work is tracked in GitHub Issues and the `FocusLauncher Stabilization` project board:

- Project: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

This file records verified state only; it is not a backlog.
