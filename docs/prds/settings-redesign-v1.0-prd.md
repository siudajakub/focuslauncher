# Settings Redesign - Product Requirements Document (PRD)

## Requirements Description

### Background

- **Business Problem**: Current settings feel hard to scan, overloaded, and not clearly divided. FocusLauncher has moved beyond its Kvaesitso base, but settings still feel like a mixed technical control panel instead of a calm, focus-first product surface.
- **Target Users**: FocusLauncher users configuring focus behavior, app visibility, launcher appearance, search, gestures, localization, backup, and advanced options.
- **Value Proposition**: Settings should help users find the right control faster, feel less overwhelmed, and understand what each setting does without needing product knowledge.

### Feature Overview

- Split settings into two distinct entry points:
  - **Focus settings** for focus mode, app classification, visibility, friction, schedules, habits, reports, and setup.
  - **Launcher settings** for homescreen, appearance, search, gestures, language, calendar, backup, and advanced launcher options.
- Surface both entries anywhere the app currently offers a generic settings entry, including the launcher overflow menu.
- Replace automatic Quick Start presentation in settings with an explicit status card and launch button.
- Keep Quick Start available during first-run onboarding with a richer walkthrough and animations in a future onboarding slice.
- Use clearer category names and short descriptions to explain what settings do.
- Prefer shallow navigation: one category level is the default; deeper screens are allowed only when a category needs meaningful substructure.

### User Scenarios

- A user opens the launcher overflow menu and sees separate entries for Focus settings and Launcher settings.
- A user opens Focus settings and immediately sees a calm status dashboard: current focus state, Quick Start status/action, app setup status, and schedule/report hints.
- A user wants to change app blocking behavior and finds it under Focus settings, not mixed with general search or appearance.
- A user wants to change wallpaper, icons, gestures, language, or backup and finds those under Launcher settings.
- A new user can still encounter Quick Start during first-run onboarding, but returning users are not interrupted by automatic Quick Start sheets in settings.

## Product Decisions

### Confirmed Direction

- Settings should feel like FocusLauncher, not a Kvaesitso-derived technical settings dump.
- Do not mention Kvaesitso to users.
- Use two separate settings entries instead of one monolithic settings screen.
- Focus settings should behave like its own center.
- Launcher settings should contain normal launcher configuration without exposing legacy framing.
- First-level screens should have a few large categories.
- Options that can be combined safely should be grouped to reduce perceived count.
- The UI tone should be calm, minimalist, or moderately information-dense when useful.

### Focus Settings Structure

1. **Status**
   - Focus enabled/off state.
   - Quick Start status card with explicit launch button.
   - Setup completeness hints, such as app classification and schedule state.

2. **Focus Mode**
   - Enable Focus mode.
   - DND behavior.
   - Quick actions and related essentials.

3. **Apps**
   - Essential apps.
   - Distracting apps.
   - Temporary access behavior.

4. **Visibility**
   - Hide or fade distracting apps.
   - No-icons mode.
   - Focus Home visibility choices.

5. **Friction and Blocking**
   - Launch friction.
   - Adaptive friction.
   - Recovery and protection options currently spread across focus support.

6. **Schedules and Day**
   - Daily schedule.
   - Schedule dock.
   - Daily habits.
   - Productivity windows and time rules where applicable.

7. **Reports**
   - Focus report.
   - Insights and history.
   - Guidance summaries.

### Launcher Settings Structure

1. **Home Screen**
   - Layout.
   - Search bar.
   - Wallpaper behavior.
   - Animations.
   - System bars.

2. **Appearance**
   - Color schemes.
   - Icons.
   - Shapes.
   - Typography.
   - Transparency.

3. **Search**
   - Search sources.
   - Ranking.
   - Hidden items.
   - Favorites.

4. **Gestures**
   - Gesture bindings and guarded actions.

5. **Language and Calendar**
   - Locale.
   - Calendar settings.
   - Measurement and related regional settings.

6. **Backup**
   - Import/export.
   - Restore flows.

7. **Advanced**
   - Debug-adjacent or rarely used launcher controls.
   - Build info and diagnostics should remain available but not prominent.

## Detailed Requirements

### Entry Points

- Replace generic settings entry points with two choices: Focus settings and Launcher settings.
- In the launcher overflow menu, show both entries.
- Existing direct routes such as Focus Report, Focus Apps, and Focus System should be audited. Keep direct shortcuts only when they still make sense beside the new Focus settings entry.
- Any external or internal route to generic settings should land on an intentional screen, not an accidental mixed list.

### Quick Start

- Focus settings must not auto-open the Quick Start sheet.
- Quick Start must be opened only through explicit user action inside settings.
- The Focus status area should include a compact Quick Start status card.
- The card should include:
  - Short explanatory text.
  - Status such as not started, partly configured, or ready, if available from current state.
  - Primary action to launch Quick Start.
  - Optional secondary action to revisit or reset setup only if there is a clear implementation path.
- First-run onboarding can still show Quick Start, but that should be treated as a separate onboarding experience, not the settings behavior.

### Content and Copy

- Add short descriptions for categories and important settings.
- Avoid technical or inherited product language.
- Canonical English copy belongs in `core/i18n/src/main/res/values/strings.xml`.
- Compose UI must use string resources, not hardcoded user-facing strings.

### Navigation and Information Architecture

- Prefer shallow screens with clearly grouped controls.
- Most settings should be reachable in one tap after choosing Focus settings or Launcher settings.
- Deeper screens are acceptable for dense editors, theme detail pages, app lists, or advanced diagnostic surfaces.
- Do not remove settings simply to reduce count; combine or reposition low-priority settings when it improves comprehension.

### Technical Constraints

- Keep settings UI in `app/ui`.
- Preserve focus sources of truth:
  - `focusEssentialAppKeys`
  - `focusDistractingAppKeys`
  - `FocusTemporaryUnlock`
  - active focus sessions and history repositories
- Do not introduce a second focus-state model.
- Reuse existing settings routes, Koin view models, repositories, preferences wrappers, and `PreferenceScreen` components where practical.
- Follow `DESIGN_SYSTEM.md` and existing Compose theme conventions.

## Acceptance Criteria

### Functional Acceptance

- [ ] Launcher overflow menu exposes separate Focus settings and Launcher settings entries.
- [ ] Other generic settings entry points are audited and updated to expose or route to the two-setting model.
- [ ] Focus settings opens to a Focus center with a status dashboard.
- [ ] Launcher settings opens to non-Focus launcher categories.
- [ ] Quick Start no longer opens automatically when entering Focus settings.
- [ ] Quick Start remains available through an explicit button in Focus settings.
- [ ] Focus categories are reorganized around Status, Focus Mode, Apps, Visibility, Friction and Blocking, Schedules and Day, and Reports.
- [ ] Launcher categories are reorganized around Home Screen, Appearance, Search, Gestures, Language and Calendar, Backup, and Advanced.
- [ ] Important settings and categories include concise explanatory summaries.

### Quality Standards

- [ ] Existing direct Focus routes still work or are intentionally redirected.
- [ ] Focus policy, app classification, and launch routing behavior are unchanged by the settings restructure.
- [ ] No user-facing strings are hardcoded in Compose.
- [ ] The UI supports dark mode through `MaterialTheme.colorScheme`.
- [ ] Relevant unit tests are updated or added for Quick Start auto-promotion behavior and route decisions.
- [ ] `tools/check_agent_docs.py` passes if documentation routing is affected.
- [ ] Relevant Gradle UI tests or compile checks pass.

### User Acceptance

- [ ] A user can tell whether they are configuring Focus behavior or general launcher behavior before entering settings.
- [ ] A returning user is not interrupted by Quick Start when opening settings.
- [ ] A new or uncertain user can still find Quick Start quickly.
- [ ] The settings structure feels calmer, simpler, and more like FocusLauncher than the inherited Kvaesitso base.

## Execution Phases

### Phase 1: Inventory and Routing Audit

**Goal**: Map current settings entry points and existing routes before changing UI.

- [ ] Audit launcher overflow actions and all direct settings intents.
- [ ] Inventory current settings routes in `SettingsActivity`.
- [ ] Identify which routes move under Focus settings versus Launcher settings.
- [ ] Decide which direct Focus shortcuts remain in the overflow menu.

**Deliverables**: Route map and implementation checklist.

### Phase 2: Two Settings Entry Points

**Goal**: Introduce the product-level split.

- [ ] Add or repurpose routes for Focus settings root and Launcher settings root.
- [ ] Update overflow menu and other settings entry points.
- [ ] Keep compatibility for existing direct route extras.
- [ ] Add strings for new entry titles and summaries.

**Deliverables**: Two working settings roots.

### Phase 3: Focus Settings Center

**Goal**: Make Focus settings readable and intentional.

- [ ] Build Focus status dashboard.
- [ ] Replace automatic Quick Start sheet behavior with explicit button behavior.
- [ ] Reorganize current Focus screens into the approved categories.
- [ ] Add category and setting descriptions.
- [ ] Verify Focus policy behavior is unchanged.

**Deliverables**: Focus settings center with explicit Quick Start.

### Phase 4: Launcher Settings Reorganization

**Goal**: Make non-Focus settings easier to scan.

- [ ] Move homescreen, appearance, search, gestures, language/calendar, backup, and advanced routes under Launcher settings.
- [ ] Combine low-priority options where existing screens are unnecessarily fragmented.
- [ ] Add summaries to category entries.
- [ ] Keep advanced and diagnostics available but not prominent.

**Deliverables**: Launcher settings root with calm category structure.

### Phase 5: Verification and Polish

**Goal**: Prove the redesign works without regressions.

- [ ] Run targeted UI/unit tests.
- [ ] Run `./gradlew :app:ui:testDebugUnitTest`.
- [ ] Run `./gradlew :app:app:assembleDefaultDebug` if feasible.
- [ ] Run `python3 tools/check_agent_docs.py` if docs changed beyond this PRD.
- [ ] Manually inspect settings navigation on a device or emulator if available.

**Deliverables**: Verified implementation and updated project status if the implementation changes product state.

## Open Questions

- Should direct overflow shortcuts like Focus Report, Focus Apps, and Focus System remain, or should they collapse under the new Focus settings entry?
- Should first-run onboarding be implemented in the same slice or tracked as a later feature after settings are reorganized?
- Should the Focus dashboard show live counts and state summaries immediately, or start with static status text and evolve later?

---

**Document Version**: 1.0
**Created**: 2026-06-14
**Clarification Rounds**: 5
**Quality Score**: 92/100
