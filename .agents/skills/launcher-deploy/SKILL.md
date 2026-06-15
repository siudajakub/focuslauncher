---
name: launcher-deploy
description: Compile, install, and set FocusLauncher as the default home app on a connected device via ADB.
---

# launcher-deploy

Use this skill when the user asks to compile the application, install it on a connected device, and set it as the default launcher. It automates the Gradle build process and uses ADB to assign the required system roles.

## Instructions

1. **Verify device connection**
   Run `adb devices` to ensure an Android device or emulator is connected and accessible.

2. **Compile and Install**
   Compile and install the default debug build on the device using Gradle. Since the project requires JDK 21, you must set `JAVA_HOME` explicitly:
   ```bash
   export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   export GRADLE_USER_HOME="$PWD/.gradle-home"
   ./gradlew :app:app:installDefaultDebug
   ```

3. **Set as Default Launcher**
   Use ADB to assign the Home role to the newly installed debug application:
   ```bash
   adb shell cmd role add-role-holder android.app.role.HOME de.mm20.launcher2.debug
   ```

4. **Verify (Optional)**
   Simulate a home button press to verify the launcher appears:
   ```bash
   adb shell input keyevent 3
   ```
