# Material Expressive UI Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the new focus UI, focus settings, and related surfaces with the project's Material 3 Expressive baseline while preserving the Focus Gate animation and gradient treatment.

**Architecture:** Reuse the existing settings scaffold and launcher theme, introduce shared section styling for focus panels, then refactor divergent screens to use calmer surface hierarchy and clearer action emphasis. Keep Focus Gate visually expressive in the background only, and move the interactive layer back toward Material components and tonal surfaces.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Navigation3, existing preference wrappers

---

### Task 1: Stabilize shared focus section styling

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`

- [ ] Introduce a shared section container and header treatment for focus home panels.
- [ ] Replace mixed plain cards and ad hoc copy styling with one calmer section pattern.
- [ ] Rebalance session actions so one CTA is primary and alternatives are secondary.

### Task 2: Bring focus settings back to preference-first structure

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/search/SearchSettingsScreen.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focushabits/DailyHabitsSettingsScreen.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusschedule/DailyScheduleSettingsScreen.kt`
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focusreport/FocusReportSettingsScreen.kt`

- [ ] Simplify the main focus settings entry screen so it routes to sub-screens instead of presenting one oversized custom section.
- [ ] Convert rich settings rows and inline content toward stable preference-like presentation.
- [ ] Keep advanced pickers and editors, but frame them with calmer Material spacing and surfaces.

### Task 3: Polish Focus Gate without removing its dramatic motion

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`

- [ ] Preserve the launch animation and gradient background.
- [ ] Move the interactive foreground onto Material-aligned surfaces and button hierarchy.
- [ ] Replace custom high-contrast control styling with theme-aware expressive styling.
- [ ] Improve compact-layout readability and spacing.

### Task 4: Verify implementation

**Files:**
- Verify modified files above

- [ ] Run targeted compilation or tests for touched UI modules.
- [ ] Review diffs for accidental behavioral regressions in unrelated in-progress work.
- [ ] Summarize remaining risks if full verification is blocked.
