# Pixel 8 Focus Smoke Test

Repeatable manual validation for launcher startup, the focus gate, focus sessions, app launch, time reminders, and crash recovery on a Pixel 8. Run it for PR and release validation of any launch-interception, lifecycle, or focus-policy change.

## Prerequisites

- Pixel 8 in Developer mode with USB debugging on, authorized for this host.
- `adb` on `PATH`; `adb devices -l` lists the device as `device`.
- A fresh debug APK built with the Verification environment exports:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew :app:app:assembleDefaultDebug
```

- Test apps classified before running: at least one Essential and one Distracting app in Focus System settings (`focusEssentialAppKeys`, `focusDistractingAppKeys`).
- For time reminders, grant Usage Access (`PACKAGE_USAGE_STATS`) and notifications (`POST_NOTIFICATIONS`).
- For strict interception, grant Usage Access and enable the FocusLauncher Accessibility service after accepting the in-app disclosure.
- For Steps on Android 10+, grant Physical activity (`ACTIVITY_RECOGNITION`); keep a device without a step sensor or a denied-permission pass available for fallback validation.

## Checklist

Use a fresh build for each pass. Tick every item or record the failure.

### Install & first launch

- [ ] `adb install -r` succeeds and reports `Success`.
- [ ] Cold launch reaches Focus Home without a crash dialog.
- [ ] The launcher is selectable as Home and survives pressing Home.
- [ ] Search opens and returns results.

### Essential app launch

- [ ] An Essential app launches immediately from a search result.
- [ ] Enter / best-match launch of the same app behaves identically.
- [ ] No focus friction or gate is shown for the Essential app.

### Distracting app blocked

- [ ] Launching a Distracting app shows the focus gate, not the app.
- [ ] The gate explains why the app is blocked.
- [ ] Cancelling the gate returns to the launcher with the app unopened.
- [ ] Tap, Enter, and hidden-item/settings launch paths all reach the same gate.

### Temporary unlock

- [ ] Steps counts live to the configured target, pauses after five seconds without a step, and keeps completed progress.
- [ ] Denying Physical activity or using a device without a step sensor offers a Delay fallback of at least ten seconds.
- [ ] Delay uses the effective adaptive/escalating delay; Tap requires confirmation before a separate `Start break` action.
- [ ] Cancelling any challenge does not create a temporary unlock.
- [ ] `Start break` opens the Distracting app for the selected 5, 10, 15, or 30 minutes, within the configured cap.
- [ ] A relaunch within the unlock window opens directly, without the gate.
- [ ] After the unlock window expires, the gate returns for that app.
- [ ] No expiry notification appears after leaving the app or while it remains foreground.

### Strict system interception

- [ ] The disclosure accurately says that only foreground app identity/state is processed locally and that screen content is not read or sent.
- [ ] Opening a blocked Distracting app from a notification, deep link, Recents, and another app performs Home and shows the same gate.
- [ ] After challenge completion, `Start break` opens the app's main launcher activity; the original deep-link destination is not restored.
- [ ] During an active temporary unlock, those system entry paths are not interrupted.
- [ ] When a temporary unlock expires while the app remains foreground, FocusLauncher returns to Home and shows the gate again.
- [ ] Force-stopping/restarting the process and reconnecting Accessibility preserves expiry enforcement through the worker recovery path.
- [ ] Revoking Accessibility or Usage Access fails open for external launches; launcher-originated launches remain gated.
- [ ] FocusLauncher, Settings, System UI, permission controller, default dialer, alarms/emergency surfaces, Essential apps, and Normal apps are never intercepted.
- [ ] A package also installed in a work/private profile is not system-intercepted, while its personal launcher entry still follows launcher policy.

### Active focus session lock

- [ ] Starting a focus session persists it and applies DND only when allowed.
- [ ] Distracting apps stay gated for the whole session.
- [ ] Essential apps still launch during the session.
- [ ] Gate continuation respects the active session, not a stale decision.

### Session expiry & restart recovery

- [ ] At expiry the session ends, scheduling is cancelled, and DND is restored only if the launcher still owns the filter.
- [ ] Force-stopping and relaunching the launcher mid-session reconciles to the same session without duplicating it.
- [ ] Re-running an already-ended session is idempotent; no duplicate end or DND change.
- [ ] After a reboot the launcher restarts cleanly with focus state intact.

### Time reminders

- [ ] Enabling reminders starts `TimeBlindnessService`; settings request Usage Access and notifications.
- [ ] Keeping a Distracting app foregrounded triggers a nudge notification at the configured boundary; tapping it opens the check-in overlay.
- [ ] Switching to a Normal or Essential app before the boundary prevents the stale reminder.
- [ ] Revoking Usage Access stops the service instead of continuing empty foreground queries.
- [ ] The nudge is a soft reminder; it does not force-close the app.
- [ ] Reminders restart after `BOOT_COMPLETED`.

### Crash diagnostics

- [ ] No `data_app_crash` entries are logged across the run.
- [ ] If startup fails, `dumpsys activity exit-info` and DropBox capture the cause.
- [ ] Any crash is reproduced and root-caused before sign-off.

## Commands reference

Reuse these exact commands; do not invent package names or paths.

```bash
adb devices -l
adb install -r app/app/build/outputs/apk/default/debug/app-default-debug.apk
adb shell am start -W -n com.siudajakub.focuslauncher.debug/de.mm20.launcher2.ui.launcher.LauncherActivity
adb shell dumpsys activity exit-info com.siudajakub.focuslauncher.debug
```

When a startup failure disappears from logcat, read the crash record:

```bash
adb shell dumpsys dropbox --print data_app_crash
```

## Referencing this checklist

Link this file from the PR description or release notes and state which pass completed (for example, "Pixel 8 smoke test: all sections green") as evidence for launch-interception, lifecycle, and focus-policy changes.
