# Project Status

Last reviewed: 2026-06-30
Branch reviewed: `dist/prep-1.0` (off `main`)
Status: distribution preparation. The stabilization and integrations-removal work (including issue #49) is merged to `main`. Distribution prep for a first public GitHub Releases build — own app identity, version 1.0.0, release signing, tagged-release CI, rebranded privacy/store docs, and a hard exported-schema CI gate — is committed on `dist/prep-1.0`, not yet merged.

## Current Product State

- The launcher is being reduced from general-purpose Kvaesitso toward an apps-first, focus-first product.
- Focus app classification uses global essential and distracting key sets.
- Focus sessions, temporary unlocks, launch friction, daily limits, focus history, and weekly focus insights exist in the current tree.
- Time awareness is wired end to end: a Time Awareness settings section (enable toggle, interval, usage-access and notification permission prompts) with `TimeBlindnessService` and `TimeBlindnessReceiver` that start on app launch and on toggle, on a safe foreground-service path for API 34+.
- Quick Capture is lossless: notes persist and list locally, with optional share.
- Search surfaces matching pinned shortcuts; browser/PWA "add to home screen" shortcuts are tagged with a (web) label.
- The settings menu is now organized into a two-item hub: Focus Settings and Launcher Settings, replacing the previous monolithic structure.
- Calculator, website search, Wikipedia, Nextcloud, and Owncloud modules are removed from the active Gradle graph in the current tree. Their dead preference wrappers (`CalculatorSearchSettings`, `WebsiteSearchSettings`, `WikipediaSearchSettings`) and five orphaned persisted fields are now also removed; the DataStore serializer's `ignoreUnknownKeys = true` makes this safe for existing installs.
- The integrations decision (#4) is made: the **Feed, Contacts, Files, and Locations** subsystems are now physically removed, including the `:data:files`, `:data:contacts`, `:data:locations`, `:services:feed`, `:services:accounts`, and `:libs:webdav` modules and their WebDAV/account backends. The core `File`/`Contact`/`Location` searchable interfaces and the plugin SDK contract are kept (no producers remain); the live Integrations settings screen (Tasks/Todoist), storage permissions, and `GenericFileProvider` are kept for sharing/backup.
- Retained per the decision: Calendar and Widgets (core); Weather, Music, and Unit conversion (advanced-only / opt-in); the Plugin SDK (developer-only). Rationale recorded in `docs/engineering/integrations-decision.md`.
- Focus data and services — classification, policy, sessions, history, session runtime, and the session-expiry worker — live in the `:services:focus` module and are now wired through a Koin `focusModule` with constructor injection (no `KoinComponent` self-injection). `FocusReviewModels` has been moved into `:services:focus` (decoupled from `app/ui`'s `R` via `core/i18n`). `FocusLaunchCoordinator` now also lives in `:services:focus`: the former circular dependency on `app/ui`'s `FocusGateActivity` was inverted via a `FocusGateLauncher` interface (with a platform-free `LaunchBounds` type) implemented by `app/ui`'s `FocusGateLauncherImpl` and supplied at the construction boundary through a parameterized `focusModule` factory; `:services:focus` now depends on `:services:favorites` (acyclic). Issue #49 is complete.

## Architecture Snapshot

- `FocusProfile` is no longer an active model. Legacy focus attributes are cleaned by database migration `35 -> 36`.
- App classification source of truth: `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Temporary access source of truth: `FocusTemporaryUnlock` in custom attributes.
- Session lifecycle (`:services:focus`): `FocusSessionRepository`, `FocusSessionRuntime`, `FocusSessionExpiryWorker` scheduling, and `FocusPolicyService`.
- Launch policy: `FocusPolicyService` (`:services:focus`), coordinated from `FocusLaunchCoordinator` (`:services:focus`), which opens the gate through the `FocusGateLauncher` interface implemented in `app/ui`.

## Distribution Readiness

First public channel is GitHub Releases (signed APK); not Google Play or F-Droid.

- App identity: `applicationId = com.siudajakub.focuslauncher` (own identity; was upstream `de.mm20.launcher2`). The `.release` applicationId suffix is dropped so the public package is clean; `.debug`/`.nightly` still coexist with an installed release.
- Version: `versionName = 1.0.0`, default `versionCode = 10000`; the nightly workflow keeps its date-based `VERSION_CODE_OVERRIDE`.
- Signing: the `release` build type now uses the env-based `gh-actions` signing config (was inheriting the debug key). The keystore and secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) are owner-held in CI.
- Release CI: `.github/workflows/release.yml` runs tests, then builds, signs, and publishes `assembleDefaultRelease` to a generated GitHub Release on a `v*` tag.
- Exported-schema drift is now a hard CI gate derived from the live `AppDatabase` version (currently 37); pre-37 schemas are intentionally not backfilled (runtime migration tests cover 24→37).
- Product docs rebranded to FocusLauncher: `docs/privacy-policy.md` rewritten for the local-only focus feature set, fastlane store descriptions updated, and the readme install section reflects the GitHub Releases channel. Kvaesitso fork attribution is retained.
- The launcher icon is already a custom Focus Launcher adaptive mark (navy home/dock motif in `core/base/src/main/res`, `minSdk = 26` so adaptive-only), not the upstream search icon.
- R8/minify is intentionally off for 1.0.0: `proguard-rules.pro` has no keep rules for Koin/kotlinx.serialization/Room/Compose, so enabling it needs a vetted rule set plus on-device testing (post-1.0 follow-up).
- The release-readiness review pass is complete (independent code + build/release review): no code blockers. Follow-ups it surfaced are addressed — the privacy policy now accurately discloses Weather/location/network and the real permission set, a dead upstream `kvaesitso.mm20.de` deep link was removed, unused declared permissions (accounts, call, external-storage family, media-location) were pruned from the manifest, and the release-CI GitHub Actions are pinned to commit SHAs.
- Signing: configured and validated. The four CI secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) are set for a fresh PKCS12 release key (alias `focuslauncher`, RSA-4096). A `release.yml` `workflow_dispatch` dry run (run 28457207599, 2026-06-30) built and signed `assembleDefaultRelease` and uploaded the artifact — the publish step is correctly skipped without a tag. The keystore file and credentials are owner-held and must be backed up (loss prevents future app updates). (This also unblocks the scheduled nightly, which had been failing at signing.)
- Pixel 8 (Android 17) smoke test, partial (2026-06-30, new build + `com.siudajakub.focuslauncher.debug` identity): install, cold launch to Focus Home, and default-Home survival verified with no crash. The interactive focus-policy sections (distracting gate, temporary unlock, session expiry/recovery, time reminders, reboot) still need a manual pass per `docs/engineering/pixel-smoke-test.md`.

## Verification Snapshot

Refreshed on 2026-06-28 with JDK 21 on `stabilize/focus-launcher`, after the backlog-clearing wave (Koin DI, `FocusReviewModels` move, focus lifecycle tests, migration hardening, dead-preference removal, focus-copy i18n).

- `python3 tools/check_agent_docs.py`: passed; it also validates the session scripts and the `SessionStart`/`PreCompact` hook wiring.
- `./gradlew test :app:app:assembleDefaultDebug :data:database:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. All module unit tests pass, `app/app/build/outputs/apk/default/debug/app-default-debug.apk` is produced, and the instrumented DB migration tests compile.
- After the Feed/Contacts/Files/Locations removals, `./gradlew test :app:app:assembleDefaultDebug`: BUILD SUCCESSFUL; the APK is produced from the reduced module graph.
- `./gradlew :services:focus:testDebugUnitTest`: BUILD SUCCESSFUL; 73 focus tests pass (59 prior + 14 new session-lifecycle tests covering start, manual end, scheduled expiry, idempotent stale-worker runs, and restart recovery).
- Pixel smoke test: not re-run in this wave; the last recorded run was 2026-06-14. The step-by-step checklist now lives in `docs/engineering/pixel-smoke-test.md`.
- `:data:database:connectedDebugAndroidTest`: not run locally (needs an Android emulator); the hardened `Migration_35_36`/`Migration_36_37` tests compile and are covered by CI.
- Distribution wave (2026-06-30, `dist/prep-1.0`): `./gradlew test :app:app:assembleDefaultDebug` BUILD SUCCESSFUL (all module unit tests pass, APK produced); `:app:app:assembleDefaultRelease --dry-run` configures cleanly with the `gh-actions` release signing config; `python3 tools/check_agent_docs.py` passes. A real signed release build is exercised only in CI (needs the owner-held keystore secrets).

The working tree is clean and the stabilization work is committed, but the branch is not yet merged to `main` and has not had a full release verification, so this snapshot does not mean the branch is release-ready.

## Work Tracking

Product and cleanup work is tracked in GitHub Issues and the `FocusLauncher Stabilization` project board:

- Project: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

This file records verified state only; it is not a backlog.
