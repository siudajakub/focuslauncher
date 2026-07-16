# Code Review And Optimization Backlog — 2026-07-16

## Scope And Status

This report records the review of `feature/focus-enhancements-2026-07` at `4d9ddf3d`, including
correctness, architecture, launch routing, persistence, background work, latency, rendering, battery
use, and documentation. It separates confirmed defects from costs that still need device
measurement.

The branch is not ready for merge or release. GitHub Issues own implementation and completion
state; this file preserves the evidence and recommended order of work.

## Superseded By Deletion (2026-07-16)

After this review the owner decided to delete the features behind several findings rather than
repair them. The evidence below is kept as the record of why, but these are now closed by removal:

- **#97 (system grayscale snapshot/restore)** — grayscale is gone entirely, launcher and system.
  The launcher no longer touches display saturation, and `WRITE_SECURE_SETTINGS` is out of the
  manifest. The unfixable part of this finding — making a `Settings.Secure` write crash-safe from a
  launcher — is exactly why the feature went.
- **#99 (Time Awareness foreground detection)** — Time Awareness is gone, taking with it the
  always-on foreground service and its per-minute `UsageStats` poll, the app's single largest
  battery cost.
- **#101 (hidden currency worker, weather scheduling)** — weather, the currency converter and the
  unit converter are deleted, so both WorkManager jobs, the ECB fetch, and the fresh-location wait
  no longer exist. The launcher now performs no periodic background work at all.
- **#103 (plugin SDK version drift)** — the plugin system, the `de.mm20.launcher2:shared`
  publication, and the `pluginSdk` version are removed, so there is no version to drift.
- **The grayscale rendering benchmark in #106** — nothing left to measure.

Still open on their original terms: **#98** (pinned shortcuts bypass focus policy), **#100**
(unlock logging durability and mixed event kinds), **#102** (partly addressed — the focus-history
indexes and the shared UsageStats repository landed; gate/reporting I/O and retention remain),
**#104** (addressed: the crash reporter now files against this fork), **#105**, **#107**.

## Release Blockers

| Priority | Finding | Evidence | Tracking | Required outcome |
| --- | --- | --- | --- | --- |
| P1 | System grayscale stores two parts of its previous-state snapshot through fire-and-forget preference writes, then immediately changes `Settings.Secure`. Process death or a rapid session restart can leave monochrome enabled or restore the wrong accessibility mode. | `FocusPolicyService.kt:412-449`, `SearchUiSettings.kt:801-809` | [#97](https://github.com/siudajakub/focuslauncher/issues/97) | Persist and clear one atomic, awaited snapshot; preserve user changes; test re-entry, process recovery, and device restore. |
| P1 | Pinned `shortcut` and `legacyshortcut` favorites bypass focus policy because `FocusLaunchCoordinator` directly launches every non-`Application` item. | `SearchVM.kt:238-289`, `FocusLaunchCoordinator.kt:39-40` | [#98](https://github.com/siudajakub/focuslauncher/issues/98) | Resolve the owning application and profile before launch, distinguish true web apps, preserve custom labels, and test tap and Enter paths. |
| P1 | Time Awareness reads only the last minute of `UsageEvents`, resets foreground state each tick, and compares a package name with full `app://package:activity` keys. Continuous distracting-app use therefore fails to accumulate reliably. | `TimeBlindnessService.kt:150-200` | [#99](https://github.com/siudajakub/focuslauncher/issues/99) | Use shared classifier semantics, reconstruct continuous foreground sessions, and schedule the next threshold instead of polling every minute. |
| P1 | Gate unlock logging is fire-and-forget, so process death can lose an event before the app opens. Weekly and session summaries also count resume or resisted events as unlocks. | `FocusHistoryRepository.kt:177-179,308-395`, `FocusGateActivity.kt:976-993`, `FocusHomeComponent.kt:674-696` | [#100](https://github.com/siudajakub/focuslauncher/issues/100) | Acknowledge durable unlock storage before launch and define unlock metrics as exactly `FocusEventKind.Unlock`. |
| P1 | An unreachable currency converter schedules an hourly ECB download by default. Weather observes fields written by its worker and enqueues non-unique immediate work, allowing redundant scheduling and network/location activity. | `LauncherSettingsData.kt:180-181`, `UnitConverterRepository.kt:41-47`, `CurrencyRepository.kt:104-111`, `WeatherRepository.kt:73-91,136-140` | [#101](https://github.com/siudajakub/focuslauncher/issues/101) | Stop work for hidden features; make weather jobs unique and constrained; observe configuration inputs only; bound fresh-location waits. |

## Other Correctness And Product Findings

| Priority | Finding | Tracking |
| --- | --- | --- |
| P2 | Focus event/session tables grow without retention or query indexes. Gate and reporting paths load and aggregate more rows than needed. | [#102](https://github.com/siudajakub/focuslauncher/issues/102) |
| P2 | Home and Insights perform separate day-wide UsageStats scans. Package-only aggregation loses component/profile semantics when classifications differ between profiles. | [#102](https://github.com/siudajakub/focuslauncher/issues/102) |
| P2 | Focus policy is evaluated again inside the gate, with repeated DataStore `.first()` calls and history reads. The UI should carry a coherent, short-lived decision context. | [#102](https://github.com/siudajakub/focuslauncher/issues/102) |
| P2 | The active-session `minutesRemaining` value changes only when `focusSessionEndsAt` emits, so the countdown can remain stale. The wind-down timer also needs lifecycle-aware boundary scheduling. | [#102](https://github.com/siudajakub/focuslauncher/issues/102) |
| P2 | Search does not consistently load pinned-shortcut custom labels, and focus visibility state is not fully reactive to the master focus toggle. | [#98](https://github.com/siudajakub/focuslauncher/issues/98) |
| P2 | The crash reporter still opens the upstream Kvaesitso issue tracker. | [#104](https://github.com/siudajakub/focuslauncher/issues/104) |
| P2 | New focus insight durations and summary separators are hard-coded in Kotlin, producing partially untranslated UI. | [#107](https://github.com/siudajakub/focuslauncher/issues/107) |
| P3 | Gradle declares plugin SDK 2.2.0 while source and documentation contain 2.3 material. | [#103](https://github.com/siudajakub/focuslauncher/issues/103) |
| P3 | The documentation toolchain reports four high and three moderate dependency advisories. These do not affect the Android APK. | [#105](https://github.com/siudajakub/focuslauncher/issues/105) |

## Optimization Plan

### 1. Remove Confirmed Background Waste

Implement [#101](https://github.com/siudajakub/focuslauncher/issues/101) first. Cancel the exchange-rate
worker while tools remain excluded from launcher search. If currency conversion returns later, run a
network-constrained daily job only while the feature is enabled.

Make immediate weather refresh unique, constrain network work, and derive scheduling from provider,
location mode, and other configuration inputs—not `lastUpdate` or `lastLocation`, which the worker
writes. Prefer cached location and use a short timeout for a fresh fix.

Expected result: no periodic network work for unreachable features and at most one periodic plus one
immediate weather job.

### 2. Reduce Gate And Reporting I/O

Implement the data work in [#102](https://github.com/siudajakub/focuslauncher/issues/102):

- add Room indexes for event time, app/time, session start, and status/start;
- replace full-row aggregation with SQL counts and projections;
- define retention or compact historical rows into rollups;
- read one coherent settings/history snapshot for a policy decision;
- pass a short-lived decision context into the gate and re-evaluate only after relevant state changes;
- expose one shared weekly-report flow per view model.

Benchmark gate latency with 1,000, 10,000, and 100,000 history rows before and after the change.

### 3. Share UsageStats Work

Create one singleton, single-flight UsageStats repository for Focus Home and Insights. Cache the
current day's aggregate for a short TTL, invalidate it at midnight and on explicit refresh, and keep
application/profile classification semantics when mapping package-level platform data.

Measure repeated Home resumes. The target is one platform scan per cache window, not one scan per
consumer or subscription.

### 4. Replace Polling With Boundary Scheduling

Fix Time Awareness under [#99](https://github.com/siudajakub/focuslauncher/issues/99). Carry the
foreground session across event windows, suspend while the screen is off, and wake at the next
configured reminder threshold.

Use one lifecycle-aware clock for wind-down transitions and the active-session countdown. Schedule
the next actual boundary rather than maintaining an unconditional minute ticker.

### 5. Measure Search, Rendering, And Media Hot Paths

These costs need measurements before broad behavioral changes. Track them in
[#106](https://github.com/siudajakub/focuslauncher/issues/106).

- Benchmark typing and best-match launch with at least 200 and 500 applications. Measure synchronous
  fuzzy scoring, sorting, cancellation, allocations, and frame time before adding a debounce.
- Compare launcher grayscale off/on with FrameTimeline, GPU, and memory counters, including wallpaper
  blur. The full-window hardware layer may be cheap enough, so keep it unless measurements show a
  regression.
- Keep media playback position in memory. Persist it at controller/lifecycle boundaries or a much
  lower cadence instead of writing SharedPreferences every second.
- Cache installed media-player packages and invalidate the cache on package changes instead of
  repeating PackageManager scans when notification state changes.

## Recommended Delivery Order

1. Fix release correctness and privacy: #97–#101.
2. Optimize persistence, gate evaluation, UsageStats, and lifecycle timers: #102.
3. Run search, grayscale, and media benchmarks, then implement measured changes: #106.
4. Close compatibility and product-quality gaps: #103–#105 and #107.
5. Run the complete device verification gate before release.

## Verification Required After Implementation

- JVM unit tests for policy, mixed event metrics, Time Awareness timelines, scheduler uniqueness, and
  locale-aware formatting.
- Room migration tests for every new index or retention change.
- `./gradlew test :app:app:assembleDefaultDebug` with JDK 21.
- Pixel smoke tests for all launch paths, session recovery, secure grayscale restore, and reboot.
- Perfetto or Macrobenchmark traces for gate latency, search typing, Home resume, and grayscale.
- Battery Historian or equivalent evidence for foreground-service wakeups and WorkManager jobs.

The 2026-07-16 review verified the existing JVM tests, default debug build, agent-documentation
check, VitePress build, and diff check. It did not run connected migration tests, Pixel smoke tests,
Perfetto, or Battery Historian.
