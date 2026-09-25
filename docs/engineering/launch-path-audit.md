# Launch Path Audit

Audit of every app launch and app-visibility entry point for focus-policy consistency
(GitHub issue #2). The single source of truth for a launch decision is
`FocusPolicyService.evaluate()`, which classifies the app key through `FocusAppClassifier`
and returns a `FocusPolicyDecision`. The single launch router is `FocusLaunchCoordinator`,
which calls `evaluate()` for `Application` items and either launches directly or opens
`FocusGateActivity`. Visibility (hide/fade/ranking) is derived from the same
`FocusAppClassifier` classification, not from ad-hoc key reads.

## Review Matrix

| Entry point | Where the decision is made (`file:line`) | Classification/policy used | Routes through central policy? | Verdict |
| --- | --- | --- | --- | --- |
| Search result tap (apps, list/grid) | `app/ui/.../search/common/grid/GridItem.kt:119`, `.../common/list/ListItem.kt:90` → `SearchableItemVM.launch` `.../common/SearchableItemVM.kt:158,173` → `FocusLaunchCoordinator.launch` `.../focus/FocusLaunchCoordinator.kt:39` | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| Best-match / launch-on-enter | `app/ui/.../search/SearchVM.kt:131` (`launchBestMatchOrAction`) → `FocusLaunchCoordinator.launch:39` | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| Home essentials / web-apps / schedule-dock cards | `app/ui/.../scaffold/FocusHomePanels.kt:638,667,685` render `SearchResultGrid` → `GridItem.kt:119` → `SearchableItemVM.launch:158` → coordinator | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| Hidden-items settings launch | `app/ui/.../settings/hiddenitems/HiddenItemsSettingsScreenVM.kt:59` → `FocusLaunchCoordinator.launch:39` | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| Gesture launch (swipe/long-press app) | `app/ui/.../scaffold/LaunchComponent.kt:54` → `FocusLaunchCoordinator.launch:39` | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| App detail toolbar "Launch" action | `app/ui/.../search/apps/AppItem.kt:403` → `SearchableItemVM.launch:158` → coordinator | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent |
| App-shortcut child launch | `app/ui/.../search/common/SearchableItemVM.kt:199` (`launchChild`) → `FocusLaunchCoordinator.launch:39` | Shortcuts are not `Application`; launch directly (classification is app-scoped) | N/A (non-app) | Intentional exception |
| Focus-gate continuation (`Start break`) | `app/ui/.../focus/FocusGateActivity.kt` → `FocusLaunchCoordinator.launchDirect` after challenge and `evaluate` | `FocusPolicyService.evaluate` resolved the gate; only `Start break` sets `FocusTemporaryUnlock`, schedules expiry, and launches direct | Yes (gate already evaluated policy) | Consistent |
| Focus-gate fast path (no gate required) | `app/ui/.../focus/FocusGateActivity.kt:402` → `launchDirect:76` | Guarded by `decision.requiresGate` from `evaluate:392` | Yes | Consistent |
| Focus-home resume-context launch | `app/ui/.../scaffold/FocusHomeComponent.kt` (`acceptResumeContext`) evaluates current policy; gated cases route through `FocusLaunchCoordinator`, allowed cases launch direct | `FocusPolicyService.evaluate` → `FocusAppClassifier` | Yes | Consistent; recovery context is cleared only after a successful direct launch |
| Browse visibility (hide distracting) | `app/ui/.../search/SearchVM.kt:200,211,222` and `.../common/SearchableItemVM.kt:115` (`hideFromBrowse`) | `FocusAppClassifier.classify` + `focusHideDistractingApps` + temporary-unlock | Yes (same classifier) | Consistent |
| Search ranking (focus weighting) | `app/ui/.../search/SearchVM.kt:302,344` (`applyRanking`/`focusAdjustment`) | `FocusAppClassifier.classify` | Yes (same classifier) | Consistent |
| Settings "open Tasks app" | `app/ui/.../settings/tasks/TasksSettingsScreenVM.kt:54` (`app.launch` direct) | None | No (bypass) | Intentional exception |
| Settings "open Smartspacer app" | `app/ui/.../settings/smartspacer/SmartspacerSettingsScreenVM.kt:41` (`app.launch` direct) | None | No (bypass) | Intentional exception |
| Notification, deep link, Recents, or another app | Accessibility window event → `FocusSystemInterceptionService.onForegroundPackage` → `FocusPolicyService.evaluate` → `FocusGateLauncher` | Unique personal-profile app plus central policy; duplicate and protected package filters | Yes | Consistent when strict mode and both permissions are enabled; otherwise deliberate fail-open |
| Temporary-unlock expiry while app remains foreground | in-process deadline or `AppSessionExpiryWorker` → `FocusSystemInterceptionService.reconcileForeground` | Usage Access foreground package → the same interception and policy path | Yes | Consistent and idempotent |
| Time-blindness foreground nudge | `app/ui/.../focus/TimeBlindnessService.kt` | Resolves one personal app key and calls `FocusAppClassifier.classifyWith` using the current Essential and Distracting sets | Yes (same classifier) | Consistent |

## Gaps And Recommendations

No known normal user-facing app launch or strict-mode foreground path bypasses the central focus
policy. System interception intentionally does not attempt to recreate a notification or deep-link
destination: after the challenge it opens the app's launcher activity. Packages that are ambiguous
across activities or profiles remain a documented strict-mode fail-open case.

Intentional exceptions (no change needed, documented for completeness):

- `TasksSettingsScreenVM.kt:54` and `SmartspacerSettingsScreenVM.kt:41` launch a fixed helper app
  (`org.tasks`, Smartspacer) directly from a settings screen to verify install/integration. These
  are configuration affordances, not user app launches; gating them through the focus gate would be
  surprising. Acceptable as-is.
- Non-`Application` searchables (shortcuts, files, contacts, calendar, locations) launch directly
  through `FocusLaunchCoordinator.launchDirect` because focus classification is app-scoped; there is
  no app `key` to classify. Consistent with the model.
