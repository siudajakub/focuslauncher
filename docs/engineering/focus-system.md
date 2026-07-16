# Focus System

## Product Model

The focus system uses global app classification plus session and policy state. Do not reintroduce the removed per-app `FocusProfile` model.

### Sources Of Truth

- Essential apps: `focusEssentialAppKeys`.
- Distracting apps: `focusDistractingAppKeys`.
- Temporary app access: `FocusTemporaryUnlock` through `CustomAttributesRepository`.
- Active session persistence: `FocusSessionRepository` and Room focus-session records.
- Session reconciliation helpers: `FocusSessionRuntime.kt`.
- Launch decision: `FocusPolicyService`.
- Classification: `FocusAppClassifier`.
- Launch routing: `FocusLaunchCoordinator` (in `:services:focus`) and launcher entry points; it opens the focus gate through the `FocusGateLauncher` interface, implemented by `app/ui`'s `FocusGateLauncherImpl` and injected at construction (a parameterized `focusModule` factory) to keep `:services:focus` free of an `app/ui` compile dependency.
- History and reports: `FocusHistoryRepository` and the focus event/session DAOs. Real foreground
  time comes from `UsageStatsManager` in `app/ui` and is reduced to platform-free models in
  `:services:focus`.

## Policy Inputs

Policy may consider classification, active focus session, productivity windows, temporary unlocks, daily limits, habits, adaptive friction, repeated launches, and recent attention history. New inputs must remain deterministic, local, testable, and explainable to the user.

## Launch Safety Matrix

When policy or visibility changes, audit all applicable paths:

- Tap on a search result.
- Enter or best-match launch.
- Focus Home and essential-app surfaces.
- Hidden-item and settings launch flows.
- Searchable customization sheets.
- Shortcuts or alternate launcher actions.
- Direct focus-gate continuation.

Every path must classify the same app key and use the same policy decision. Avoid one-off checks in composables or activities.

## Session Lifecycle

Session start must persist the session, update preference projections, schedule expiry, and apply DND only when allowed. Session end or expiry must reconcile the expected session, cancel scheduling where appropriate, restore DND only when the launcher still owns the active filter, and clear projections.

Test process death, stale workers, repeated end calls, expired sessions, and mismatched session IDs as idempotent cases.

## Data Migration

Migration `35 -> 36` removes legacy focus custom attributes while preserving `FocusTemporaryUnlock` payloads. Migration `37 -> 38` drops the tables of the removed weather/currency/plugin features and adds the focus-history indexes (`FocusEvent` by time and by app+time; `FocusSession` by start and by status+start). Any future persistence change must include migration tests and exported schema updates.

## No Background Poller

The launcher runs no foreground service and no periodic background work. The only scheduled work is
two one-shot `WorkManager` jobs with an initial delay — `FocusSessionExpiryWorker` and
`AppSessionExpiryWorker` — each firing once at a known boundary. Do not reintroduce a poller: an
always-on service was previously the app's single largest battery cost, for a feature that a
launcher cannot enforce anyway (it can neither block nor close a foreground app).

The gate "time" sets a `FocusTemporaryUnlock` and an `AppSessionExpiryWorker` notification — a
reminder, never a hard block.

## Reflection

- Leaving a distracting-app gate without launching records `FocusEventKind.Resisted`. Turn-aways
  must remain separate from unlock metrics, budget, friction, and drift signals. Some current
  aggregate queries still mix resume events into unlock/session totals; correction and durable
  logging are tracked in [issue #100](https://github.com/siudajakub/focuslauncher/issues/100).
- Focus Insights and the Focus Home show today's foreground time for distracting apps using
  UsageStats. This is read-only, local, and hidden when Usage Access yields no data. Focus Settings
  offers the Usage Access grant.
- Both consumers read through the single `FocusUsageStatsRepository`, which single-flights the
  platform scan behind a short day-scoped cache. Query UsageStats through it, never directly: a
  whole-day `queryAndAggregateUsageStats` is an expensive blocking Binder call, and it used to run
  once per consumer per input emission.
- The launcher never manipulates display saturation. Grayscale — launcher-level or system-wide via
  the secure daltonizer — was removed deliberately: that setting belongs to the user, and driving
  `Settings.Secure` from a launcher could not be made crash-safe. `WRITE_SECURE_SETTINGS` is not in
  the manifest and must not come back.

## Known Launch-Path Gap

Pinned Android shortcuts are represented as favorites and can currently reach the direct-launch
branch without being classified as their owning application. Until
[issue #98](https://github.com/siudajakub/focuslauncher/issues/98) is fixed, shortcut launches are a
known focus-policy bypass. Treat every new launch surface as policy-sensitive even if its
`Searchable` type is not `Application`.

## UI Principles

- Keep the launcher calm and apps-first.
- Explain why friction changed or why an app is blocked.
- Keep recommendations sparse and reversible.
- Do not make essential apps inherit distracting-app friction.
- Follow `DESIGN_SYSTEM.md` and canonical i18n rules.
