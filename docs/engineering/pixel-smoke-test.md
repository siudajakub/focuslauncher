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

- [ ] Choosing temporary access from the gate opens the Distracting app.
- [ ] A relaunch within the unlock window opens directly, without the gate.
- [ ] After the unlock window expires, the gate returns for that app.
- [ ] The expiry notification (`AppSessionExpiryWorker`) appears for the gated "time" path.

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
- [ ] Keeping a Distracting app foregrounded triggers a nudge notification.
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
adb shell am start -W -n de.mm20.launcher2.debug/de.mm20.launcher2.ui.launcher.LauncherActivity
adb shell dumpsys activity exit-info de.mm20.launcher2.debug
```

When a startup failure disappears from logcat, read the crash record:

```bash
adb shell dumpsys dropbox --print data_app_crash
```

## Referencing this checklist

Link this file from the PR description or release notes and state which pass completed (for example, "Pixel 8 smoke test: all sections green") as evidence for launch-interception, lifecycle, and focus-policy changes.
