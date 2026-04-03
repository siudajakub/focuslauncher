# FocusLauncher ADHD Focus OS Master Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:dispatching-parallel-agents only for disjoint write scopes. Keep one shared focus-state system. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the launcher-centered ADHD Focus OS roadmap as one coordinated local-first rollout without turning the app into a generic planner or cloud product.

**Architecture:** Reuse the existing focus stack in `FocusHome`, `FocusGateActivity`, `FocusPolicyService`, `FocusHistoryRepository`, `SearchUiSettings`, and `LauncherSettingsData`. Add three small extensions on top of that base: a block-attached planning layer keyed by `date + normalized block label`, a local attention/environment reasoning layer, and a deterministic report/recommendation layer. Keep all smart behavior pure, explainable, reversible, and settings-backed where user impact is high.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, Flow/StateFlow, Room, DataStore-backed launcher preferences, existing `CalendarRepository`, existing focus helper-model tests in `app/ui`.

---

## Program Shape

### Shared foundations already present in the current tree

- Daily schedule snapshots, prep guidance, schedule-aware recovery, and block-aware session sizing already exist in:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusSupportModels.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Focus support settings and schedule-alignment report basics already exist in:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusreport/`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- The master rollout should build on those changes instead of replacing them.

### Missing program layers to finish

- Phase 2 block action plans
- Phase 3 adaptive protection and rescue heuristics beyond simple escalation
- Phase 4 environment snapshot and explainability
- Phase 5 weekly/monthly review, recommendations, and experiment mode

---

## File Structure

### Preferences and pure models

- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/FocusSettings.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModels.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModels.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModels.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModelsTest.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModelsTest.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModelsTest.kt`

### Home and launcher surfaces

- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockSetupSheet.kt`

### Policy, history, report, and persistence

- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt`
- Modify: `data/database/src/main/java/de/mm20/launcher2/database/entities/FocusEventEntity.kt`
- Modify: `data/database/src/main/java/de/mm20/launcher2/database/AppDatabase.kt`
- Create: `data/database/src/main/java/de/mm20/launcher2/database/migrations/Migration_35_36.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusreport/FocusReportSettingsScreen.kt`
- Modify: `core/i18n/src/main/res/values/strings.xml`

---

## Task 1: Extend Shared Local Models and Settings

**Files:**
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/FocusSettings.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModels.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModels.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModels.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModelsTest.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModelsTest.kt`
- Test: `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModelsTest.kt`

- [ ] Add serializable block-plan types keyed by `date + normalized block label`.
- [ ] Add serializable experiment and recommendation state with one active experiment at a time.
- [ ] Add serializable environment preferences for context-aware behavior and explainability toggles.
- [ ] Add pure helper logic for block-plan matching, stale-plan invalidation, readiness state, adaptive friction profile resolution, environment reason resolution, and recommendation heuristics.
- [ ] Write and run focused JVM tests for the new helper models before wiring UI.

## Task 2: Add Block Action Plans and Ready-To-Start Home Guidance

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Create: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockSetupSheet.kt`
- Modify: `core/i18n/src/main/res/values/strings.xml`

- [ ] Add a lightweight block setup flow attached to current/upcoming schedule blocks.
- [ ] Surface intention, first tiny step, optional note, optional app suggestion set, and readiness checklist tied to habits/prep.
- [ ] Reuse unfinished tiny steps when the same normalized block label reappears later the same day.
- [ ] Upgrade the primary home guidance card to show `Recover`, `Prep`, `Ready`, `Now`, or no primary guidance.
- [ ] Add entrypoints from `FocusHome`, `FocusGate`, and the daily schedule card without creating a separate planner.

## Task 3: Upgrade Adaptive Protection and Recovery

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `core/i18n/src/main/res/values/strings.xml`

- [ ] Add a local attention-state summary from recent unlocks, mismatches, abandoned sessions, launcher bounce-backs, missed habits, and repeated block interruptions.
- [ ] Resolve adaptive friction profiles `light`, `normal`, and `strict` from deterministic heuristics with a grace window.
- [ ] Make repeated mismatched launches and same-app same-block drift meaningfully harder while aligned launches stay softer.
- [ ] Add targeted recovery copy, one-click block reset, and session rescue suggestions on home.
- [ ] Keep visible local explanations for friction changes in the gate and home UI.

## Task 4: Add Environment Snapshot and Explainability

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
- Modify: `core/i18n/src/main/res/values/strings.xml`

- [ ] Add a local environment snapshot combining time-of-day modes plus available device signals that are already local and cheap to explain.
- [ ] Start with time-of-day and charging state, then fold in existing commute/media signals only where the codebase already has stable access.
- [ ] Add a `What shaped this screen?` disclosure sheet listing active schedule, habits, environment, and protection rules.
- [ ] Use environment only to refine launcher guidance and optional surfaces, never to replace calendar + habits.

## Task 5: Add Weekly Review, Recommendations, and Experiment Mode

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- Modify: `data/database/src/main/java/de/mm20/launcher2/database/entities/FocusEventEntity.kt`
- Modify: `data/database/src/main/java/de/mm20/launcher2/database/AppDatabase.kt`
- Create: `data/database/src/main/java/de/mm20/launcher2/database/migrations/Migration_35_36.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusreport/FocusReportSettingsScreen.kt`
- Modify: `core/i18n/src/main/res/values/strings.xml`

- [ ] Extend local history and aggregations to support weekly and monthly review by block label, app key, hour bucket, and event kind.
- [ ] Keep any `FocusEvent` schema extension minimal and only add fields that materially unlock reports or heuristics.
- [ ] Add deterministic recommendations for friction, unlock caps, prep prompts, prep lead time, and habit timing.
- [ ] Add one-at-a-time experiment mode with local before/after metrics shown in the report surface.
- [ ] Limit recommendations to one or two at a time to avoid overwhelm.

## Task 6: Verification and Integration

**Files:**
- Modify only if integration or migration issues are found during verification

- [ ] Run focused helper-model JVM tests for all new model files.
- [ ] Run `:app:ui:compileDebugKotlin`.
- [ ] Run `:app:app:assembleDefaultDebug`.
- [ ] If Room schema changes, update exported schema and validate migration behavior.
- [ ] Re-check roadmap acceptance criteria phase by phase against the final diff, not only against green builds.

---

## Parallel Ownership

### Main rollout

- Own Task 1 shared settings/model integration.
- Own final integration of all worker patches.
- Own any merge-conflict resolution in `LauncherSettingsData.kt`, `SearchUiSettings.kt`, and `strings.xml`.

### Worker A: Block Planning

- Own:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModels.kt`
  - `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockPlanModelsTest.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusBlockSetupSheet.kt`
- May touch:
  - `FocusHomeComponent.kt`
  - `FocusHomePanels.kt`
  - `FocusGateActivity.kt`

### Worker B: Adaptive Protection

- Own:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- May touch:
  - `FocusGateActivity.kt`
  - `FocusHomeComponent.kt`

### Worker C: Environment + Review

- Own:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModels.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModels.kt`
  - `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusEnvironmentModelsTest.kt`
  - `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusReviewModelsTest.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusreport/FocusReportSettingsScreen.kt`
- May touch:
  - `FocusHomePanels.kt`
  - `FocusHomeComponent.kt`

### Shared-file warning

- `FocusHomeComponent.kt`
- `FocusHomePanels.kt`
- `FocusGateActivity.kt`
- `LauncherSettingsData.kt`
- `SearchUiSettings.kt`
- `strings.xml`

Do not let two workers edit the same shared file in parallel without explicit ownership reassignment.

---

## Verification Commands

- [ ] Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:ui:testDebugUnitTest --tests 'de.mm20.launcher2.ui.launcher.focus.*'
```

- [ ] Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:ui:compileDebugKotlin
```

- [ ] Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

---

## Acceptance Checklist

- [ ] Home shows at most one primary guidance card and can elevate `Ready to start` when a block plan is complete.
- [ ] Block plans stay local, attached to date + normalized block label, and do not leak across unrelated days.
- [ ] Repeated drift meaningfully increases friction without punishing aligned launches.
- [ ] Important behavior shifts are explainable from within the launcher.
- [ ] Weekly/monthly report surfaces behavior-shaping signals, not just raw counts.
- [ ] Recommendations stay sparse, deterministic, local, and reversible.
