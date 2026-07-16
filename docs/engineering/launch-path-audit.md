# Launch Path Audit

Last reviewed: 2026-07-16 on `feature/focus-enhancements-2026-07`.

`FocusPolicyService.evaluate()` is the app decision source and `FocusLaunchCoordinator` in
`:services:focus` is the shared router. `FocusGateLauncherImpl` in `app/ui` opens the Android gate
without creating a service-to-UI dependency.

## Expected Matrix

| Item state | Expected launch behavior |
| --- | --- |
| Essential or unclassified app | Direct launch unless another explicit policy input applies. |
| Distracting app | Evaluate policy and open the gate. |
| Active temporary unlock | Direct launch while the unlock is valid. |
| Active focus-session lock, hard window, habit deadline, or exhausted budget | Gate with the matching explanation; never bypass through an alternate app surface. |
| Non-application searchable | Direct launch only when it cannot resolve to a classified parent application. |

## Entry-Point Matrix

| Entry point | Current route | Verdict |
| --- | --- | --- |
| Search result tap / app detail launch | `GridItem` or `ListItem` → `SearchableItemVM.launch` → coordinator | Consistent for `Application`. |
| Enter / best match | `SearchVM.launchBestMatchOrAction` → coordinator | Consistent for application results. |
| Focus Home app cards | Shared search grid/list → `SearchableItemVM` → coordinator | Consistent for applications. |
| Hidden-items settings | `HiddenItemsSettingsScreenVM.launch` → coordinator | Consistent for applications. |
| Gesture-bound app | `LaunchComponent.onActivate` → coordinator | Consistent for applications. |
| Gate continuation | Gate evaluates policy, creates temporary access, then uses `launchDirect` | Intentional post-gate direct launch. |
| Resume-context acceptance | `FocusHomeVM.acceptResumeContext` → `launchDirect` after explicit recovery acceptance | Intentional post-acceptance direct launch. |
| Tasks.org / Smartspacer configuration | Dedicated settings view model calls the fixed helper app directly | Documented configuration exception. |
| Pinned shortcut / legacy shortcut | Search/Home → coordinator, which sends every non-`Application` directly | **Gap:** a shortcut that opens a distracting app can bypass session, habit, and budget policy. |
| Time Awareness foreground classification | `TimeBlindnessService` compares a package name with full application keys | **Gap:** the comparison uses incompatible key formats and reminders do not reliably classify apps. |

## Visibility And Ranking Findings

- `SearchVM` reads `focusModeEnabled` and `focusHideDistractingApps` once when a search job starts.
  A long-lived empty-query collector does not react when either setting changes, so browse results
  can stay stale until the job/view model restarts.
- `SearchableItemVM.hideFromBrowse`, fade, and no-icons state do not include the focus master toggle.
  Cards can remain visually de-emphasized when Focus mode is off even though `SearchVM` itself uses
  the master toggle for filtering/ranking.

## Required Follow-Up Design

- Resolve a shortcut's owning package/component to an `Application` key (and profile) before launch,
  or explicitly exclude non-web shortcuts from the PWA surface. Route resolved shortcuts through
  the same policy while preserving direct launch for true non-app items.
- Give Time Awareness the same classifier/key conversion as launch policy and derive continuous
  foreground duration from UsageEvents instead of comparing package names with `app://...` keys.
- Make focus-enabled, hide/fade/no-icons, classification, and temporary unlocks part of one reactive
  visibility state shared by browse, ranking, and item rendering.

These are open review findings, not approved exceptions. Implementation and status belong in
GitHub Issues.
