# Launchly ADHD Support & Protection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ADHD-first daily support features that make it easier to start tasks, recover after distraction, stay aware of time, and increase protection against impulsive launches.

**Architecture:** Extend the existing focus-first launcher instead of adding a separate “mode”. Reuse `FocusHome`, `FocusGate`, `FocusPolicyService`, `FocusHistoryRepository`, and the new `Daily schedule` / `Daily habits` / `Schedule dock` foundation. Add one shared assistant-state layer in preferences + small pure helper models for predictable logic, then plug that into home UI, gate flows, and policy evaluation.

**Tech Stack:** Kotlin, Jetpack Compose, Flow/StateFlow, existing `LauncherDataStore` preferences layer, existing `CalendarRepository`, existing `FocusGateActivity`, existing `FocusHomeComponent`.

---

## File Structure

### Existing files to extend

- `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
  New persisted settings for start support, recovery support, time-awareness support, and harder protection.
- `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
  New flows and mutators for the added settings.
- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt`
  Policy precedence for stricter protection and mismatch detection.
- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
  Start ritual, micro-step prompt, stricter friction, context-aware blocked copy.
- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
  New home cards: recovery, time warnings, reset button, current task support.
- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
  UI components extracted from `FocusHomeComponent`.
- `app/ui/src/main/java/de/mm20/launcher2/ui/settings/homepanels/HomePanelsSettingsScreen.kt`
  Add configuration entrypoints for the new home-support panels.
- `app/ui/src/main/java/de/mm20/launcher2/ui/settings/search/SearchSettingsScreen.kt`
  Keep only focus-core controls; avoid panel creep.
- `core/i18n/src/main/res/values/strings.xml`
  New copy for all new flows and cards.

### New files to create

- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModels.kt`
  Pure data classes and helper logic for start ritual, recovery memory, transition warnings, and stricter protection state.
- `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreen.kt`
  One settings screen for assistant behavior: start ritual, recovery, time-awareness, stricter protection.
- `app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreenVM.kt`
  State and mutators for the settings screen.
- `app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModelsTest.kt`
  Pure logic tests for the new ADHD support helpers.

### Optional file split if `FocusGateActivity.kt` becomes too large

- `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGatePanels.kt`
  Extracted composables for start ritual / blocked states / recovery copy if file size becomes unwieldy.

---

## Task 1: Add Shared ADHD Support Settings & Pure Models

**Files:**
- Create: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModels.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
- Test: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModelsTest.kt`

- [ ] **Step 1: Write the failing tests for pure helper logic**

```kotlin
package de.mm20.launcher2.ui.launcher.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusAssistantModelsTest {
    @Test
    fun `resolve transition warning becomes active 5 minutes before block end`() {
        val warning = resolveTransitionWarning(
            minutesUntilBlockEnd = 4,
            nextBlockLabel = "Admin",
        )
        assertTrue(warning.show)
        assertEquals("Admin", warning.nextBlockLabel)
    }

    @Test
    fun `resolve launch escalation increases only for repeated launches within cooldown window`() {
        val state = resolveLaunchEscalation(
            launchTimestamps = listOf(1_000L, 40_000L, 70_000L),
            nowMillis = 75_000L,
            windowMillis = 90_000L,
        )
        assertEquals(3, state.recentAttempts)
        assertTrue(state.extraDelaySeconds > 0)
    }

    @Test
    fun `resume card shows only if last context was interrupted recently`() {
        val card = resolveResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Writing",
                interruptedAtMillis = 100_000L,
            ),
            nowMillis = 120_000L,
            expiryMillis = 60_000L,
        )
        assertTrue(card.show)
        assertEquals("Writing", card.taskLabel)
    }

    @Test
    fun `resume card expires after timeout`() {
        val card = resolveResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Writing",
                interruptedAtMillis = 100_000L,
            ),
            nowMillis = 200_000L,
            expiryMillis = 60_000L,
        )
        assertFalse(card.show)
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail because models do not exist yet**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:ui:testDebugUnitTest --tests de.mm20.launcher2.ui.launcher.focus.FocusAssistantModelsTest
```

Expected:
- `FocusAssistantModelsTest` compile fails on unresolved references like `resolveTransitionWarning`

- [ ] **Step 3: Add persisted settings for ADHD support**

Add these fields to `LauncherSettingsData`:

```kotlin
val focusStartRitualEnabled: Boolean = true,
val focusMicroStepPromptEnabled: Boolean = true,
val focusRecoveryEnabled: Boolean = true,
val focusRecoveryResumeTimeoutMinutes: Int = 30,
val focusTransitionWarningsEnabled: Boolean = true,
val focusTransitionWarningLeadMinutes: Int = 5,
val focusEscalatingFrictionEnabled: Boolean = true,
val focusEscalationWindowMinutes: Int = 15,
val focusEscalationExtraDelaySeconds: Int = 10,
val focusDistractingSessionCapMinutes: Int = 15,
val focusResetButtonEnabled: Boolean = true,
val focusLastResumeContext: FocusResumeContext? = null,
```

Also add:

```kotlin
@Serializable
data class FocusResumeContext(
    val taskLabel: String,
    val scheduleBlockLabel: String? = null,
    val microStep: String? = null,
    val appKey: String? = null,
    val interruptedAtMillis: Long,
)
```

- [ ] **Step 4: Expose the new settings in `SearchUiSettings`**

Add flows + setters for:

```kotlin
val focusStartRitualEnabled
val focusMicroStepPromptEnabled
val focusRecoveryEnabled
val focusRecoveryResumeTimeoutMinutes
val focusTransitionWarningsEnabled
val focusTransitionWarningLeadMinutes
val focusEscalatingFrictionEnabled
val focusEscalationWindowMinutes
val focusEscalationExtraDelaySeconds
val focusDistractingSessionCapMinutes
val focusResetButtonEnabled
val focusLastResumeContext

fun setFocusStartRitualEnabled(enabled: Boolean)
fun setFocusMicroStepPromptEnabled(enabled: Boolean)
fun setFocusRecoveryEnabled(enabled: Boolean)
fun setFocusRecoveryResumeTimeoutMinutes(minutes: Int)
fun setFocusTransitionWarningsEnabled(enabled: Boolean)
fun setFocusTransitionWarningLeadMinutes(minutes: Int)
fun setFocusEscalatingFrictionEnabled(enabled: Boolean)
fun setFocusEscalationWindowMinutes(minutes: Int)
fun setFocusEscalationExtraDelaySeconds(seconds: Int)
fun setFocusDistractingSessionCapMinutes(minutes: Int)
fun setFocusResetButtonEnabled(enabled: Boolean)
fun setFocusLastResumeContext(context: FocusResumeContext?)
```

- [ ] **Step 5: Implement pure helper models**

Create `FocusAssistantModels.kt` with:

```kotlin
data class TransitionWarningState(
    val show: Boolean,
    val nextBlockLabel: String? = null,
)

data class LaunchEscalationState(
    val recentAttempts: Int,
    val extraDelaySeconds: Int,
)

data class ResumeCardState(
    val show: Boolean,
    val taskLabel: String? = null,
    val microStep: String? = null,
)

fun resolveTransitionWarning(
    minutesUntilBlockEnd: Int,
    nextBlockLabel: String?,
    leadMinutes: Int = 5,
): TransitionWarningState { ... }

fun resolveLaunchEscalation(
    launchTimestamps: List<Long>,
    nowMillis: Long,
    windowMillis: Long,
): LaunchEscalationState { ... }

fun resolveResumeCard(
    lastContext: FocusResumeContext?,
    nowMillis: Long,
    expiryMillis: Long,
): ResumeCardState { ... }
```

Implementation rules:
- `TransitionWarningState.show == true` only if `nextBlockLabel != null` and `minutesUntilBlockEnd in 0..leadMinutes`
- `LaunchEscalationState.extraDelaySeconds`:
  - `0` for 0 or 1 recent attempts
  - `baseExtraDelay` for 2 attempts
  - `baseExtraDelay * 2` for 3+ attempts
- `ResumeCardState.show == true` only if context exists and has not expired

- [ ] **Step 6: Re-run the helper tests**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:ui:testDebugUnitTest --tests de.mm20.launcher2.ui.launcher.focus.FocusAssistantModelsTest
```

Expected:
- `BUILD SUCCESSFUL`
- `FocusAssistantModelsTest` green

- [ ] **Step 7: Commit the shared foundation**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModels.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/test/java/de/mm20/launcher2/ui/launcher/focus/FocusAssistantModelsTest.kt
git commit -m "feat: add focus assistant settings foundation"
```

## Task 2: Add `Focus Support` Settings Screen

**Files:**
- Create: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreen.kt`
- Create: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreenVM.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/SettingsActivity.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/search/SearchSettingsScreen.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml`

- [ ] **Step 1: Add a single entrypoint from `Focus mode`**

In `SearchSettingsScreen.kt`, keep `Focus mode` focused on:
- `Focus apps`
- delay
- session default
- DND
- `Productivity time`
- report
- new entry:

```kotlin
Preference(
    title = stringResource(R.string.focus_support_title),
    summary = stringResource(R.string.focus_support_summary),
    icon = R.drawable.psychology_24px,
    onClick = { backStack.add(FocusSupportSettingsRoute) },
)
```

- [ ] **Step 2: Add nav entries in `SettingsActivity`**

Add imports and destination:

```kotlin
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsRoute
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsScreen
```

and:

```kotlin
entry<FocusSupportSettingsRoute> {
    FocusSupportSettingsScreen()
}
```

- [ ] **Step 3: Implement the VM**

Expose:

```kotlin
val startRitualEnabled
val microStepPromptEnabled
val recoveryEnabled
val recoveryResumeTimeoutMinutes
val transitionWarningsEnabled
val transitionWarningLeadMinutes
val escalatingFrictionEnabled
val escalationWindowMinutes
val escalationExtraDelaySeconds
val distractingSessionCapMinutes
val resetButtonEnabled
```

with setters calling the new `SearchUiSettings` methods.

- [ ] **Step 4: Implement the settings screen**

Screen sections:

```kotlin
PreferenceCategory(title = stringResource(R.string.focus_support_start_title)) { ... }
PreferenceCategory(title = stringResource(R.string.focus_support_recovery_title)) { ... }
PreferenceCategory(title = stringResource(R.string.focus_support_time_title)) { ... }
PreferenceCategory(title = stringResource(R.string.focus_support_protection_title)) { ... }
```

Use:
- `SwitchPreference` for booleans
- `ListPreference` for minute/second values

Recommended default lists:

```kotlin
items = listOf(5, 10, 15, 20, 30).map { "$it min" to it }
```

and:

```kotlin
items = listOf(5, 10, 15, 20).map { "${it}s" to it }
```

- [ ] **Step 5: Add copy**

Add strings for:
- `focus_support_title`
- `focus_support_summary`
- `focus_support_start_title`
- `focus_support_recovery_title`
- `focus_support_time_title`
- `focus_support_protection_title`
- and each toggle/summary

Write copy as product language, not technical language. Example:

```xml
<string name="focus_support_start_ritual_title">Start ritual</string>
<string name="focus_support_start_ritual_summary">Pause before a focus session and define the first small step</string>
```

- [ ] **Step 6: Run a targeted compile check**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:ui:compileDebugKotlin
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit the settings screen**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreen.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/focussupport/FocusSupportSettingsScreenVM.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/SettingsActivity.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/settings/search/SearchSettingsScreen.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml
git commit -m "feat: add focus support settings"
```

## Task 3: Add Start Support to `FocusGate` and Session Start

**Files:**
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml`

- [ ] **Step 1: Extend the gate UI state**

Inside `FocusGateActivity.kt`, add local state:

```kotlin
var microStep by remember { mutableStateOf("") }
val startRitualEnabled by searchUiSettings.focusStartRitualEnabled.collectAsState(initial = true)
val microStepPromptEnabled by searchUiSettings.focusMicroStepPromptEnabled.collectAsState(initial = true)
```

- [ ] **Step 2: Change the intentional unlock screen**

If `startRitualEnabled` is on, the `Intent` stage should show:
- app name
- reason / intention
- optional micro-step field if `microStepPromptEnabled`
- session duration control

Add a second text field:

```kotlin
OutlinedTextField(
    value = microStep,
    onValueChange = { microStep = it },
    label = { Text(stringResource(R.string.focus_gate_micro_step_label)) },
    placeholder = { Text(stringResource(R.string.focus_gate_micro_step_placeholder)) },
)
```

- [ ] **Step 3: Require a micro-step only when prompt is enabled**

Button enabled state:

```kotlin
enabled = reason.isNotBlank() && (!microStepPromptEnabled || microStep.isNotBlank())
```

- [ ] **Step 4: Persist the chosen context for recovery**

Before launch:

```kotlin
searchUiSettings.setFocusLastResumeContext(
    FocusResumeContext(
        taskLabel = app.labelOverride ?: app.label,
        scheduleBlockLabel = null,
        microStep = microStep.takeIf { it.isNotBlank() },
        appKey = app.key,
        interruptedAtMillis = System.currentTimeMillis(),
    )
)
```

- [ ] **Step 5: Include the micro-step in focus history logging**

Expand `FocusLogEvent`:

```kotlin
val microStep: String? = null,
```

and pass it from `FocusGateActivity`.

- [ ] **Step 6: Add strings**

```xml
<string name="focus_gate_micro_step_label">First tiny step</string>
<string name="focus_gate_micro_step_placeholder">Example: open the doc, write the title, answer one message</string>
```

- [ ] **Step 7: Run the app module build**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml
git commit -m "feat: add start ritual to focus gate"
```

## Task 4: Add Recovery Support to `FocusHome`

**Files:**
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml`

- [ ] **Step 1: Derive a resume card state in `FocusHomeVM`**

Add:

```kotlin
val resumeCardState = combine(
    searchUiSettings.focusRecoveryEnabled,
    searchUiSettings.focusLastResumeContext,
    currentTime,
    searchUiSettings.focusRecoveryResumeTimeoutMinutes,
) { enabled, lastContext, nowMillis, timeoutMinutes ->
    if (!enabled) {
        ResumeCardState(show = false)
    } else {
        resolveResumeCard(
            lastContext = lastContext,
            nowMillis = nowMillis,
            expiryMillis = timeoutMinutes * 60_000L,
        )
    }
}.stateIn(...)
```

- [ ] **Step 2: Add a reset-button card state**

Expose:

```kotlin
val showResetButton = searchUiSettings.focusResetButtonEnabled.stateIn(...)
```

The reset action should:
- clear search
- clear `focusLastResumeContext`
- optionally scroll home to top if already available through scaffold state

If there is no current API to clear search globally, add a minimal callback path in `LauncherScaffold` / `FocusHomeComponent` rather than inventing a new global singleton.

- [ ] **Step 3: Add a `Resume` panel**

In `FocusHomePanels.kt`, add:

```kotlin
@Composable
internal fun FocusResumeCard(
    state: ResumeCardState,
    onDismiss: () -> Unit,
) { ... }
```

Content:
- title: `Resume`
- task label
- optional micro-step
- dismiss button

- [ ] **Step 4: Add a `Reset` button on home**

Add a simple card or button:

```kotlin
OutlinedButton(
    onClick = onReset,
    modifier = Modifier.fillMaxWidth(),
) {
    Text(stringResource(R.string.focus_home_reset))
}
```

- [ ] **Step 5: Clear stale resume context**

When user dismisses the resume card:

```kotlin
searchUiSettings.setFocusLastResumeContext(null)
```

When user starts a fresh focus session from home:

```kotlin
searchUiSettings.setFocusLastResumeContext(null)
```

- [ ] **Step 6: Add strings**

```xml
<string name="focus_home_resume_title">Resume</string>
<string name="focus_home_resume_action">Back to it</string>
<string name="focus_home_reset">Reset my focus</string>
```

- [ ] **Step 7: Run build**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml
git commit -m "feat: add recovery support to focus home"
```

## Task 5: Add Time Blindness Support to Home

**Files:**
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml`

- [ ] **Step 1: Derive transition warnings in `FocusHomeVM`**

Use the current daily schedule snapshot:

```kotlin
val transitionWarningState = combine(
    searchUiSettings.focusTransitionWarningsEnabled,
    searchUiSettings.focusTransitionWarningLeadMinutes,
    dailyScheduleState,
) { enabled, leadMinutes, dailySchedule ->
    if (!enabled) TransitionWarningState(show = false)
    else resolveTransitionWarning(
        minutesUntilBlockEnd = dailySchedule.snapshot.minutesUntilCurrentBlockEnds,
        nextBlockLabel = dailySchedule.snapshot.nextBlock?.label,
        leadMinutes = leadMinutes,
    )
}
```

- [ ] **Step 2: Render a small transition-warning card**

Show only if there is a current block and a next block:

```kotlin
Text(stringResource(R.string.focus_home_transition_title))
Text(stringResource(R.string.focus_home_transition_body, nextBlockLabel))
```

- [ ] **Step 3: Strengthen time-left presentation**

In the daily schedule card:
- keep `Now`
- keep the current block label
- keep `Ends in X min`
- increase contrast/weight of the countdown line

Do not add more than one extra line of detail; preserve calm UI.

- [ ] **Step 4: Add strings**

```xml
<string name="focus_home_transition_title">Transition coming up</string>
<string name="focus_home_transition_body">Next up: %1$s</string>
```

- [ ] **Step 5: Run build**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomePanels.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml
git commit -m "feat: add transition warnings to focus home"
```

## Task 6: Add Harder Protection

**Files:**
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt`
- Modify: `/Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml`

- [ ] **Step 1: Add recent-launch escalation state**

In `FocusHistoryRepository`, add a helper:

```kotlin
suspend fun getRecentAppLaunchTimestamps(appKey: String, sinceMillis: Long): List<Long>
```

This should read recent logged focus events for the app and return timestamps.

- [ ] **Step 2: Integrate escalation into policy**

In `FocusPolicyService.evaluate(app)`:

```kotlin
val escalatingFrictionEnabled = searchUiSettings.focusEscalatingFrictionEnabled.first()
val escalationWindowMinutes = searchUiSettings.focusEscalationWindowMinutes.first()
val escalationExtraDelaySeconds = searchUiSettings.focusEscalationExtraDelaySeconds.first()
```

For `Distracting` apps:
- fetch recent launches within the configured window
- compute `LaunchEscalationState`
- add the extra delay to `effectiveDelaySeconds`

Do not escalate for `Essential` or `Normal`.

- [ ] **Step 3: Enforce session cap**

In `FocusGateActivity`, cap the unlock duration:

```kotlin
val capMinutes by searchUiSettings.focusDistractingSessionCapMinutes.collectAsState(initial = 15)
sessionMinutes = sessionMinutes.coerceAtMost(capMinutes)
```

And when rotating through session durations, never exceed `capMinutes`.

- [ ] **Step 4: Add mismatch-focused copy**

If current daily schedule block exists and app is `Distracting`, prefer copy like:

```kotlin
stringResource(R.string.focus_gate_message_block_mismatch, currentBlockLabel)
```

Only use this when:
- a current block exists
- it is different from the app context
- app is not hard-blocked by `Productivity time` or habits

- [ ] **Step 5: Add strings**

```xml
<string name="focus_gate_message_block_mismatch">This does not fit your current block: %1$s.</string>
<string name="focus_support_escalating_friction_title">Escalating friction</string>
<string name="focus_support_session_cap_title">Session cap</string>
```

- [ ] **Step 6: Run build**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusPolicyService.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusGateActivity.kt \
  /Users/j/vibe/lanucher/kvaesitso/app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/FocusHistoryRepository.kt \
  /Users/j/vibe/lanucher/kvaesitso/core/i18n/src/main/res/values/strings.xml
git commit -m "feat: add stricter distraction protection"
```

## Task 7: Final Regression Pass on Device

**Files:**
- No code changes required unless regressions are found

- [ ] **Step 1: Build the full debug APK**

Run:

```bash
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home ./gradlew --no-daemon --console=plain :app:app:assembleDefaultDebug
```

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 2: Install on the Pixel**

Run:

```bash
adb -s 3A281FDJH00020 install -r /Users/j/vibe/lanucher/kvaesitso/app/app/build/outputs/apk/default/debug/app-default-debug.apk
```

Expected:
- `Success`

- [ ] **Step 3: Smoke test Start Support**

Manual checks:
- mark a test app as `Distracting`
- launch it
- confirm gate shows:
  - reason
  - micro-step field
  - session duration
- confirm button is disabled until required input is present

- [ ] **Step 4: Smoke test Recovery**

Manual checks:
- open a distracting app through gate with a filled micro-step
- return to launcher
- confirm a `Resume` card appears
- dismiss it
- confirm it disappears and does not immediately reappear

- [ ] **Step 5: Smoke test Time Blindness support**

Manual checks:
- pick a `Daily schedule` calendar with adjacent blocks
- verify `Now`, countdown, and `Next`
- when within warning threshold, verify transition warning appears

- [ ] **Step 6: Smoke test Harder Protection**

Manual checks:
- repeatedly open the same distracting app within the escalation window
- confirm the delay becomes longer
- confirm unlock duration does not exceed the configured cap

- [ ] **Step 7: Final commit if any regression fixes were needed**

```bash
git add <only regression-fix files>
git commit -m "fix: polish focus assistant rollout"
```

## Spec Coverage Self-Check

- `Start support` is covered by Task 3.
- `Recovery` is covered by Task 4.
- `Time blindness support` is covered by Task 5.
- `Harder protection` is covered by Task 6.
- Existing `Daily schedule`, `Daily habits`, and `Schedule dock` are reused rather than redesigned.
- The launcher remains a single ADHD-first product, not a mode toggle.

## Notes for the Next Session

- Execute this plan on a fresh worktree if possible to avoid conflicts with the already-dirty current worktree.
- Do not redesign `FocusHome` visually during this plan; only add the minimal UI needed for the new behaviors.
- If a file grows too large, prefer extracting focused composables/helpers rather than adding another giant screen file.

## Recommended Subagent Execution Order

### Phase 1 — Foundation first, no parallelism

- Task 1 must be done first.
- Reason:
  - it defines the shared settings and helper models
  - all later tasks depend on those types and flows existing

### Phase 2 — One settings worker, one launcher worker

Run these in parallel after Task 1:

- `Worker A`
  - Task 2: `Focus Support` settings screen
- `Worker B`
  - Task 3: Start support in `FocusGate`

Constraints:
- `Worker A` owns settings/navigation/copy only
- `Worker B` owns gate/history/copy only
- both may touch `strings.xml`, so merge carefully after review

### Phase 3 — Home behavior, then policy

Run these in this order:

- Task 4: Recovery support on `FocusHome`
- Task 5: Time blindness support on `FocusHome`

Reason:
- both heavily touch `FocusHomeComponent.kt` and `FocusHomePanels.kt`
- parallelizing them would create unnecessary conflicts

Then run:

- Task 6: Harder protection

Reason:
- it depends on the new recovery/time-awareness context being present
- it touches `FocusPolicyService.kt`, `FocusGateActivity.kt`, and history logic

### Phase 4 — Final verification only

- Task 7 runs last and should not overlap with implementation work.

### Suggested ownership split

- `Subagent 1: Settings`
  - Task 2 only
- `Subagent 2: Gate`
  - Task 3 and later Task 6 if desired
- `Main rollout or single focused worker: Home`
  - Task 4 and Task 5

### Review checkpoints

- After Task 1:
  - confirm helper tests are green before dispatching more work
- After Task 3:
  - confirm gate still builds and logs correctly before changing home behavior
- After Task 5:
  - run a full app build before Task 6
- After Task 6:
  - install on device and do Task 7 manually
