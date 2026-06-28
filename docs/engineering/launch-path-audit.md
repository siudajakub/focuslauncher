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
| Focus-gate continuation ("Continue") | `app/ui/.../focus/FocusGateActivity.kt:884` → `FocusLaunchCoordinator.launchDirect:76` after `evaluate` at `:392` | `FocusPolicyService.evaluate` resolved the gate; continuation sets `FocusTemporaryUnlock` then launches direct | Yes (gate already evaluated policy) | Consistent |
| Focus-gate fast path (no gate required) | `app/ui/.../focus/FocusGateActivity.kt:402` → `launchDirect:76` | Guarded by `decision.requiresGate` from `evaluate:392` | Yes | Consistent |
| Focus-home resume-context launch | `app/ui/.../scaffold/FocusHomeComponent.kt:914` (`acceptResumeContext`) → `FocusLaunchCoordinator.launchDirect:76` | None — uses `launchDirect`, skips `evaluate` | No (deliberate bypass) | Intentional exception |
| Browse visibility (hide distracting) | `app/ui/.../search/SearchVM.kt:200,211,222` and `.../common/SearchableItemVM.kt:115` (`hideFromBrowse`) | `FocusAppClassifier.classify` + `focusHideDistractingApps` + temporary-unlock | Yes (same classifier) | Consistent |
| Search ranking (focus weighting) | `app/ui/.../search/SearchVM.kt:302,344` (`applyRanking`/`focusAdjustment`) | `FocusAppClassifier.classify` | Yes (same classifier) | Consistent |
| Settings "open Tasks app" | `app/ui/.../settings/tasks/TasksSettingsScreenVM.kt:54` (`app.launch` direct) | None | No (bypass) | Intentional exception |
| Settings "open Smartspacer app" | `app/ui/.../settings/smartspacer/SmartspacerSettingsScreenVM.kt:41` (`app.launch` direct) | None | No (bypass) | Intentional exception |
| Time-blindness foreground nudge | `app/ui/.../focus/TimeBlindnessService.kt:190` | Reads `focusDistractingAppKeys` directly; prefix match on package name, not `FocusAppClassifier` | No (own classification) | GAP (classification only) |

## Gaps And Recommendations

One classification-consistency gap; no launch-gating bypass that lets a distracting app skip
the gate from a normal user-facing launch surface.

- `app/ui/.../focus/TimeBlindnessService.kt:190` classifies the current foreground app with a
  bespoke `distractingKeys.any { currentApp.startsWith(it) }` package-prefix match instead of
  `FocusAppClassifier`. This is a reminder, not a launch gate (the launcher cannot intercept an
  already-foregrounded app), so it is not a launch bypass, but the matching rule differs from the
  classifier's exact app-`key` membership and can drift. Remedy: classify the resolved foreground
  app key through `FocusAppClassifier.classifyNow`/`classifyWith` so the "distracting" definition
  matches every other surface.

Intentional exceptions (no change needed, documented for completeness):

- `TasksSettingsScreenVM.kt:54` and `SmartspacerSettingsScreenVM.kt:41` launch a fixed helper app
  (`org.tasks`, Smartspacer) directly from a settings screen to verify install/integration. These
  are configuration affordances, not user app launches; gating them through the focus gate would be
  surprising. Acceptable as-is.
- `FocusHomeComponent.kt:914` (`acceptResumeContext`) uses `launchDirect` by design: the user has
  explicitly accepted a recovery prompt for a previously-gated app, so re-gating would be redundant
  friction. The accompanying `FocusLogEvent` records the resume.
- Non-`Application` searchables (shortcuts, files, contacts, calendar, locations) launch directly
  through `FocusLaunchCoordinator.launchDirect` because focus classification is app-scoped; there is
  no app `key` to classify. Consistent with the model.

Owner decision needed: confirm whether the time-blindness package-prefix match (which can flag
sub-packages an explicit app key would not) is intended, or should be tightened to the classifier's
exact-key membership.
