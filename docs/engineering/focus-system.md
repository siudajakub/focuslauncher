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
- Launch routing: `FocusLaunchCoordinator` and launcher entry points.
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

## Session Lifecycle

Session start must persist the session, update preference projections, schedule expiry, and apply DND only when allowed. Session end or expiry must reconcile the expected session, cancel scheduling where appropriate, restore DND only when the launcher still owns the active filter, and clear projections.

Test process death, stale workers, repeated end calls, expired sessions, and mismatched session IDs as idempotent cases.

## Data Migration

Migration `35 -> 36` removes legacy focus custom attributes while preserving `FocusTemporaryUnlock` payloads. Any future persistence change must include migration tests and exported schema updates.

## UI Principles

- Keep the launcher calm and apps-first.
- Explain why friction changed or why an app is blocked.
- Keep recommendations sparse and reversible.
- Do not make essential apps inherit distracting-app friction.
- Follow `DESIGN_SYSTEM.md` and canonical i18n rules.
