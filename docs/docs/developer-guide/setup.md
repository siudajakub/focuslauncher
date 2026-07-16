---
sidebar_position: 1
---

# Setup

## Setup the build environment

- Install Android Studio, the Android SDK, and JDK 21 or newer.
- Clone the Git repository: `git clone https://github.com/siudajakub/focuslauncher`
- Open the project in Android Studio

For command-line verification, use the repository-local Gradle home:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew :app:app:assembleDefaultDebug
```
