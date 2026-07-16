# Project Status

Last reviewed: 2026-07-16

Branch reviewed: `feature/focus-enhancements-2026-07`

Status: pre-release feature branch, ahead of `origin/main`; no public GitHub Release exists yet.

## Current Product

- FocusLauncher is an apps-first Android launcher derived from Kvaesitso, with a two-entry settings
  hub: Focus Settings and Launcher Settings.
- App classification uses the global `focusEssentialAppKeys` and `focusDistractingAppKeys` sets.
  Focus policy, sessions, temporary unlocks, history, expiry, usage reflection, and launch
  coordination live in `:services:focus`.
- The branch adds turn-away reflection and UsageStats-based distracting-app time. These compile and
  have unit coverage, but still require the Pixel smoke checks in
  `docs/engineering/pixel-smoke-test.md`.
- Search returns installed applications plus pinned `shortcut`/`legacyshortcut` favorites — the set
  through which a PWA added to the home screen surfaces. No other result type exists.
  `SearchService` takes a single application repository and `SearchFilters` no longer exists. A
  pinned shortcut can still bypass focus policy because it is launched as a generic favorite rather
  than its owning app; see [issue #98](https://github.com/siudajakub/focuslauncher/issues/98).
- Quick Capture persists notes locally and supports sharing. Calendar, widgets, and music remain
  active.

## Removed On 2026-07-16

The owner decided to delete rather than repair. Weather (module, providers, widget), the currency
converter and its hourly ECB worker, the unit converter, the entire plugin system (`:plugins:sdk`,
`:services:plugins`, `:data:plugins`, the `:core:base`/`:core:shared` contracts, the
`de.mm20.launcher2:shared` publication, dokka), `:core:devicepose`, grayscale (launcher and the
system daltonizer), and Time Awareness are all gone. The orphaned `File`/`Contact`/`Location`/
`Website`/`Article` searchable types, the dead search-filter UI and settings, and the dead plugin
and cloud badge providers went with them.

This closes issues #97, #99, #101, #103 and the grayscale part of #106 by removal — see the
"Superseded By Deletion" section of
[`docs/engineering/code-review-2026-07-16.md`](docs/engineering/code-review-2026-07-16.md) for the
rationale, and [`docs/engineering/integrations-decision.md`](docs/engineering/integrations-decision.md)
for the superseding product decision.

## Background Work And Battery

- The launcher runs **no foreground service** and **no periodic background work**. The only
  scheduled jobs are the one-shot, initial-delay `FocusSessionExpiryWorker` and
  `AppSessionExpiryWorker`.
- It requests **no location**. `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`,
  `WRITE_SECURE_SETTINGS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
  `RECEIVE_BOOT_COMPLETED`, and `VIBRATE` are gone from the source manifests, and the dead
  `Location`, `Contacts`, `ExternalStorage` and `Call` groups are gone from `PermissionsManager`.
  (`WAKE_LOCK`/`RECEIVE_BOOT_COMPLETED`/`FOREGROUND_SERVICE` still merge in from the WorkManager
  library manifest; nothing in this app declares or drives them.)
- `INTERNET` is still declared and still needed: the **Todoist integration**
  (`app/ui/.../focus/todoist/TodoistClient.kt`) fetches tasks over the network when the user has
  configured an API token. That is the only outbound traffic the app makes; there is no automatic
  or background network access.
- Focus Home and Focus Insights share one `FocusUsageStatsRepository`, which single-flights the
  UsageStats scan behind a 60s day-scoped cache, replacing one full-day platform scan per consumer
  per input emission.
- The music service no longer writes SharedPreferences once per second while a track plays; the
  playback position is kept in memory and persisted at playback boundaries.

## Architecture And Persistence

- Android application ID: `com.siudajakub.focuslauncher`; source namespace remains
  `de.mm20.launcher2` for fork compatibility.
- Version: `1.0.0` (`versionCode = 10000`). Database version: **38**.
- Migration `37 -> 38` drops the `forecasts`, `Currency` and `Plugins` tables and any leftover
  weather widget row, and adds indexes on `FocusEvent(timestamp)`, `FocusEvent(appKey, timestamp)`,
  `FocusSession(startedAt)`, and `FocusSession(status, startedAt)`. The fresh-install widget seed is
  now music + calendar.
- `FocusProfile` is no longer active. Temporary access is stored as `FocusTemporaryUnlock`; active
  sessions are persisted by `FocusSessionRepository` and reconciled by `FocusSessionRuntime`.
- The focus module has no compile dependency on `app/ui`: `FocusLaunchCoordinator` opens the gate
  through the injected `FocusGateLauncher` interface.
- Release and nightly CI use the owner-held `gh-actions` signing identity. Debug uses the Android
  debug key. Release, nightly, and debug have distinct application IDs.
- R8/minification remains disabled. Enabling it requires keep-rule work and device verification.

## Open Review Findings

Full report: [`docs/engineering/code-review-2026-07-16.md`](docs/engineering/code-review-2026-07-16.md).

- Pinned Android shortcuts bypass the focus gate:
  [issue #98](https://github.com/siudajakub/focuslauncher/issues/98).
- Unlock logging can be lost and aggregate metrics mix event kinds:
  [issue #100](https://github.com/siudajakub/focuslauncher/issues/100).
- Focus data/gate optimization is partly done under
  [issue #102](https://github.com/siudajakub/focuslauncher/issues/102): the Room indexes and the
  shared UsageStats repository landed; gate re-evaluation I/O and history retention remain.
- Search and media hot paths still need device measurement:
  [issue #106](https://github.com/siudajakub/focuslauncher/issues/106) (its grayscale item is void).
- Documentation toolchain advisories:
  [issue #105](https://github.com/siudajakub/focuslauncher/issues/105); they do not affect the
  Android runtime artifact.
- Hard-coded focus-insight duration and summary formatting:
  [issue #107](https://github.com/siudajakub/focuslauncher/issues/107).

The in-app crash reporter now files against this fork rather than upstream Kvaesitso, closing
[issue #104](https://github.com/siudajakub/focuslauncher/issues/104).

## Distribution

- The planned first public channel is a signed APK on GitHub Releases. No public release exists as
  of this review.
- `.github/workflows/release.yml` builds and publishes `assembleDefaultRelease` for `v*` tags.
- A prior `workflow_dispatch` signing dry run succeeded on 2026-06-30. The keystore must remain
  backed up because losing it prevents in-place updates.
- The last recorded device smoke test was a partial Pixel 8 / Android 17 pass on 2026-06-30. It
  covered install, cold launch, and default-home survival, not the current branch's interactive
  focus paths.

## Verification Snapshot

Verified on 2026-07-16 with JDK 21, after the removals above:

- `./gradlew test :app:app:assembleDefaultDebug`: passed; the default debug APK was produced.
- `python3 tools/check_agent_docs.py`: passed.
- `npm run docs:build`: passed.

Connected Room migration tests (including the new `37 -> 38` cases), performance traces, and the
Pixel smoke checklist require a device or emulator and were not run.

## Work Tracking

- Project board: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10
- Review follow-ups: https://github.com/siudajakub/focuslauncher/issues/97 through https://github.com/siudajakub/focuslauncher/issues/107

This file records verified state only. GitHub Issues own actionable work.
