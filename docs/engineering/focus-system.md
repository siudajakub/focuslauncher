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
- Launch routing: `FocusLaunchCoordinator` (in `:services:focus`) and launcher entry points; it opens the focus gate through the `FocusGateLauncher` interface, implemented by `app/ui`'s `FocusGateLauncherImpl` and registered at the application composition root to keep `:services:focus` free of an `app/ui` compile dependency.
- Optional foreground interception: `FocusSystemInterceptionService`, fed by window-change events from `LauncherAccessibilityService` and platform operations exposed through `FocusForegroundController`.
- History and reports: `FocusHistoryRepository` and focus event DAO.

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

## Unlock Challenges

Only a plain `Distracting` classification block can be completed with an unlock challenge. Focus-session locks, hard-block windows, daily budgets, and habit blocks remain non-bypassable. The available methods are Steps, Delay, and Tap. Completing a challenge only moves the gate to `ReadyToStartBreak`; `Start break` is the sole action that writes `FocusTemporaryUnlock`, schedules expiry, logs the event, and launches the app.

Steps uses `TYPE_STEP_DETECTOR` first and `TYPE_STEP_COUNTER` second. Android 10 and newer require `ACTIVITY_RECOGNITION`. Progress pauses after five seconds without a step but is not discarded. A missing sensor or denied permission offers Delay with a minimum of ten seconds. User-selected Delay still uses `FocusPolicyDecision.effectiveDelaySeconds`, including adaptive and escalating friction.

Temporary access remains per app. The selectable break lengths are 5, 10, 15, and 30 minutes, constrained by the existing session cap; a new installation starts at 10 minutes.

## System Interception

System interception is opt-in and covers entry through notifications, deep links, Recents, and other apps. It requires both the FocusLauncher Accessibility service and Usage Access. Accessibility receives window package names only (`canRetrieveWindowContent=false`) and performs Home; Usage Access reconstructs the foreground package after process restart and at temporary-unlock expiry. Foreground package names and state are processed locally and are not sent.

`FocusSystemInterceptionService` resolves only one unambiguous personal-profile launcher app, evaluates it with `FocusPolicyService`, deduplicates repeated window events, and opens the same `FocusGateActivity` used by launcher launches. Packages also present in another profile are deliberately fail-open for system interception and remain gated when launched from FocusLauncher.

FocusLauncher itself, the current Home app, Android Settings, System UI, permission controllers, the default dialer, alarms, and common emergency packages are never intercepted. If either permission is missing—or Home cannot be performed during a permission race—system interception fails open. Launcher-originated launches still follow normal focus policy.

An in-process deadline job checks foreground state exactly when a temporary unlock expires while Accessibility is connected. `AppSessionExpiryWorker` provides durable recovery after process death. Repeated expiry checks are safe because the same policy decision and event debounce are used each time.

## Session Lifecycle

Session start must persist the session, update preference projections, schedule expiry, and apply DND only when allowed. Session end or expiry must reconcile the expected session, cancel scheduling where appropriate, restore DND only when the launcher still owns the active filter, and clear projections.

Test process death, stale workers, repeated end calls, expired sessions, and mismatched session IDs as idempotent cases.

## Data Migration

Migration `35 -> 36` removes legacy focus custom attributes while preserving `FocusTemporaryUnlock` payloads. Any future persistence change must include migration tests and exported schema updates.

## Time Awareness Reminders

`TimeBlindnessService` nudges the user while a distracting app stays in the foreground. It needs Usage Access (`PACKAGE_USAGE_STATS`) to read foreground transitions and `POST_NOTIFICATIONS` to deliver the nudge. It resolves an unambiguous personal app key and uses `FocusAppClassifier`, so Essential precedence and Distracting classification match launcher surfaces. While a distracting app remains open, the next UsageEvents query is scheduled for the reminder boundary instead of every minute; screen-off suspends the timer, and missing or revoked Usage Access stops the service. The reminder is a notification whose action opens the optional check-in overlay, avoiding restricted background activity launches. The service starts when the user enables reminders, when Usage Access is granted, on app launch, and on `BOOT_COMPLETED`. Without system interception this remains a soft reminder. With opt-in system interception, `AppSessionExpiryWorker` can also trigger foreground reconciliation and return an expired distracting app to Home.

## UI Principles

- Keep the launcher calm and apps-first.
- Explain why friction changed or why an app is blocked.
- Keep recommendations sparse and reversible.
- Do not make essential apps inherit distracting-app friction.
- Follow `DESIGN_SYSTEM.md` and canonical i18n rules.
