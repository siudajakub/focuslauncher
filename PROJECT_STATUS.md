# Project Status

Last reviewed: 2026-09-10
Branch reviewed: `main` working tree
Status: the focus-interception, unlock-challenge, home-customization, and migration wave compiles and passes JVM tests; consent-dependent device behavior still needs a full Pixel smoke pass.

## Current Product State

- The launcher is being reduced from general-purpose Kvaesitso toward an apps-first, focus-first product.
- Focus app classification uses global essential and distracting key sets.
- Focus sessions, temporary unlocks, launch friction, daily limits, focus history, and weekly focus insights exist in the current tree.
- Distracting-app launches can use Steps, Delay, or Tap challenges before a bounded temporary unlock. An opt-in strict mode also intercepts unambiguous personal-profile launches from notifications, links, Recents, and other apps when both Accessibility and Usage Access are granted; missing or revoked access fails open outside the launcher.
- Time awareness is wired end to end: a Time Awareness settings section (enable toggle, interval, Usage Access and notification prompts) with `TimeBlindnessService` and `TimeBlindnessReceiver`. The service preserves continuous foreground state across UsageEvents windows, schedules its next distracting-app check at the reminder boundary, suspends while the screen is off, stops when Usage Access is missing, and posts a notification instead of launching an activity from the background.
- Quick Capture is lossless: notes persist and list locally, with optional share.
- Search surfaces matching pinned shortcuts; browser/PWA "add to home screen" shortcuts are tagged with a (web) label.
- The settings menu is now organized into a two-item hub: Focus Settings and Launcher Settings, replacing the previous monolithic structure.
- Calculator, website search, Wikipedia, Nextcloud, and Owncloud modules are removed from the active Gradle graph in the current tree. Their dead preference wrappers (`CalculatorSearchSettings`, `WebsiteSearchSettings`, `WikipediaSearchSettings`) and five orphaned persisted fields are now also removed; the DataStore serializer's `ignoreUnknownKeys = true` makes this safe for existing installs.
- The integrations decision (#4) is made: the **Feed, Contacts, Files, and Locations** subsystems are now physically removed, including the `:data:files`, `:data:contacts`, `:data:locations`, `:services:feed`, `:services:accounts`, and `:libs:webdav` modules and their WebDAV/account backends. The core `File`/`Contact`/`Location` searchable interfaces and the plugin SDK contract are kept (no producers remain); the live Integrations settings screen (Tasks/Todoist), storage permissions, and `GenericFileProvider` are kept for sharing/backup.
- Retained per the decision: Calendar and Widgets (core); Weather, Music, and Unit conversion (advanced-only / opt-in); the Plugin SDK (developer-only). Rationale recorded in `docs/engineering/integrations-decision.md`.
- Focus data and services — classification, policy, sessions, history, session runtime, system interception, and expiry recovery — live in the `:services:focus` module and are wired through Koin with constructor injection. `FocusLaunchCoordinator` opens UI through the `FocusGateLauncher` interface implemented in `app/ui`; the implementation is a regular application-layer binding, so launch callers no longer pass it as a factory parameter. `FocusForegroundController` keeps Home/foreground operations behind `:services:global-actions`, preserving the module boundary.
- The consolidated Focus Settings screen exposes only controls with active consumers. Dormant commute, At a Glance, and environment-context preferences remain serialized for compatibility but are not presented as working features; the environment-guidance phase remains unfinished.

## Architecture Snapshot

- `FocusProfile` is no longer an active model. Legacy focus attributes are cleaned by database migration `35 -> 36`.
- App classification source of truth: `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Temporary access source of truth: `FocusTemporaryUnlock` in custom attributes.
- Session lifecycle (`:services:focus`): `FocusSessionRepository`, `FocusSessionRuntime`, `FocusSessionExpiryWorker` scheduling, and `FocusPolicyService`.
- Launch policy: `FocusPolicyService` (`:services:focus`), coordinated from `FocusLaunchCoordinator` (`:services:focus`), which opens the gate through the `FocusGateLauncher` interface implemented in `app/ui`.
- Strict interception: `FocusSystemInterceptionService` consumes package-only Accessibility window events, evaluates the same `FocusPolicyService`, and reconciles active unlock expiry with foreground state. It never reads view trees or screen content and excludes launcher/system/settings/phone/alarm/emergency surfaces.

## Distribution Readiness

First public channel is GitHub Releases (signed APK); not Google Play or F-Droid.

- App identity: `applicationId = com.siudajakub.focuslauncher` (own identity; was upstream `de.mm20.launcher2`). The `.release` applicationId suffix is dropped so the public package is clean; `.debug`/`.nightly` still coexist with an installed release.
- Version: `versionName = 1.0.0`, default `versionCode = 10000`; the nightly workflow keeps its date-based `VERSION_CODE_OVERRIDE`.
- Signing: the `release` build type now uses the env-based `gh-actions` signing config (was inheriting the debug key). The keystore and secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) are owner-held in CI.
- Release CI: `.github/workflows/release.yml` runs tests, then builds, signs, and publishes `assembleDefaultRelease` to a generated GitHub Release on a `v*` tag.
- Exported-schema drift is now a hard CI gate derived from the live `AppDatabase` version (currently 39); pre-37 schemas are intentionally not backfilled. Compatibility paths cover canonical schema 37 and the divergent schema 38 found on existing debug installs.
- Product docs rebranded to FocusLauncher: `docs/privacy-policy.md` rewritten for the local-only focus feature set, fastlane store descriptions updated, and the readme install section reflects the GitHub Releases channel. Kvaesitso fork attribution is retained.
- The launcher icon is already a custom Focus Launcher adaptive mark (navy home/dock motif in `core/base/src/main/res`, `minSdk = 26` so adaptive-only), not the upstream search icon.
- R8/minify is intentionally off for 1.0.0: `proguard-rules.pro` has no keep rules for Koin/kotlinx.serialization/Room/Compose, so enabling it needs a vetted rule set plus on-device testing (post-1.0 follow-up).
- The release-readiness review pass is complete (independent code + build/release review): no code blockers. Follow-ups it surfaced are addressed — the privacy policy now accurately discloses Weather/location/network and the real permission set, a dead upstream `kvaesitso.mm20.de` deep link was removed, unused declared permissions (accounts, call, external-storage family, media-location) were pruned from the manifest, and the release-CI GitHub Actions are pinned to commit SHAs.
- Signing: configured and validated. The four CI secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) are set for a fresh PKCS12 release key (alias `focuslauncher`, RSA-4096). A `release.yml` `workflow_dispatch` dry run (run 28457207599, 2026-06-30) built and signed `assembleDefaultRelease` and uploaded the artifact — the publish step is correctly skipped without a tag. The keystore file and credentials are owner-held and must be backed up (loss prevents future app updates). (This also unblocks the scheduled nightly, which had been failing at signing.)
- Pixel 8 (Android 17) smoke test, partial (2026-08-24): the existing debug database was migrated in place from divergent schema 38 to canonical schema 39 without clearing data; FocusLauncher cold-started as the default Home with the prior focus-session rows intact. Strict Accessibility interception, physical step counting, exact expiry, process restart, and reboot still need a consented manual pass per `docs/engineering/pixel-smoke-test.md`.

## Verification Snapshot

Refreshed on 2026-09-10 with JDK 21 on the `main` working tree.

- `python3 tools/check_agent_docs.py`: passed; `AGENTS.md` remains within its validator-enforced length.
- `./gradlew :app:ui:compileDebugKotlin :app:ui:testDebugUnitTest :services:focus:testDebugUnitTest :services:global-actions:testDebugUnitTest`: BUILD SUCCESSFUL. Coverage includes challenge state, interception routing, foreground reconstruction, preference behavior, and continuous foreground-state retention.
- `./gradlew test :app:app:assembleDefaultDebug :data:database:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. All module JVM tests pass, the debug APK is produced, and the migration test APK compiles.
- `./gradlew :app:app:lintDefaultDebug :app:app:assembleDefaultDebug`: BUILD SUCCESSFUL with zero lint errors after removing the obsolete signature-only `INTERACT_ACROSS_PROFILES` declaration; 57 non-blocking dependency/API maintenance warnings remain.
- Fresh-context Terra behavior/battery review and Luna UI/completeness review were completed against the working diff. The stale foreground reminder, policy-bypassing recovery launch, background overlay launch, lifecycle countdown, dead setting controls, recommendation toggle, contrast, accessibility, and API-level FGS findings were repaired; Terra's narrow re-review found no remaining behavioral blocker.
- After the Feed/Contacts/Files/Locations removals, `./gradlew test :app:app:assembleDefaultDebug`: BUILD SUCCESSFUL; the APK is produced from the reduced module graph.
- `./gradlew :services:focus:testDebugUnitTest`: BUILD SUCCESSFUL; 73 focus tests pass (59 prior + 14 new session-lifecycle tests covering start, manual end, scheduled expiry, idempotent stale-worker runs, and restart recovery).
- `./gradlew :data:database:connectedDebugAndroidTest`: not run fresh; the Pixel 8 was initially visible as `3A281FDJH00020` but disconnected before instrumentation, and the configured `Pixel_Test` AVD has no installed system image. The prior 2026-08-24 device evidence above remains the latest migration smoke evidence.
- Distribution wave (2026-06-30, `dist/prep-1.0`): `./gradlew test :app:app:assembleDefaultDebug` BUILD SUCCESSFUL (all module unit tests pass, APK produced); `:app:app:assembleDefaultRelease --dry-run` configures cleanly with the `gh-actions` release signing config; `python3 tools/check_agent_docs.py` passes. A real signed release build is exercised only in CI (needs the owner-held keystore secrets).

The implementation and audit repairs remain uncommitted in the working tree. Without the consent-dependent Pixel smoke scenarios and a fresh connected migration run, this snapshot does not mean the tree is release-ready.

## Work Tracking

Product and cleanup work is tracked in GitHub Issues and the `FocusLauncher Stabilization` project board:

- Project: https://github.com/users/siudajakub/projects/1
- Seeded backlog: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

This file records verified state only; it is not a backlog.
