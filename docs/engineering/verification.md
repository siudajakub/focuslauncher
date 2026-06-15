# Verification

Use fresh command output as evidence. A successful lint or partial test is not proof that the app builds or that a launcher flow works.

## Environment

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.gradle-home"
```

Use JDK 21 or newer. On macOS with Homebrew OpenJDK, this is the known-good local setup:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Verification Matrix

| Change | Minimum checks |
| --- | --- |
| Documentation or skills | `python3 tools/check_agent_docs.py` |
| Pure focus/search helpers | `./gradlew :app:ui:testDebugUnitTest :core:base:test` |
| UI or launcher integration | Relevant unit tests and `./gradlew :app:app:assembleDefaultDebug` |
| Preference model | Relevant unit tests, serialization compatibility review, debug build |
| Room schema | Unit tests, `:data:database:connectedDebugAndroidTest`, exported schema review |
| Build logic or dependencies | `./gradlew test :app:app:assembleDefaultDebug` |
| Launch interception or lifecycle | Build plus Pixel 8 smoke test |

## Pixel Smoke Test

```bash
adb devices -l
adb install -r app/app/build/outputs/apk/default/debug/app-default-debug.apk
adb shell am start -W -n de.mm20.launcher2.debug/de.mm20.launcher2.ui.launcher.LauncherActivity
adb shell dumpsys activity exit-info de.mm20.launcher2.debug
```

For focus changes, verify essential launch, distracting launch, gate continuation, active session lock, session expiry, temporary unlock, and launcher restart. Use `dumpsys dropbox --print ... data_app_crash` when startup failures disappear from logcat.

## Reporting

Final reports must name the commands run and their result. If a required check cannot run, state the blocker and avoid claiming the affected behavior is verified.

Update `PROJECT_STATUS.md` only with durable facts supported by the current tree and fresh evidence. Do not paste transient logs into status documents.
