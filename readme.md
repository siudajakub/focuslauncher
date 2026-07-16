# FocusLauncher

FocusLauncher is an Android launcher built to reduce distractions and make intentional app use easier.
It is based on Kvaesitso, with the experience being adapted toward focus sessions, calmer browsing, and
stronger friction around distracting apps.

## What It Does

- returns nothing from search but your installed apps and the web apps you pinned to the home screen
- separates essential and distracting apps behind one shared launch policy
- provides focus sessions, temporary access, daily limits, adaptive launch friction, habits, and
  calendar-aware planning
- reflects turn-aways, weekly focus history, and optional on-device foreground time
- keeps Quick Capture notes locally, with sharing only on explicit action
- runs no background service, asks for no location, and goes online only for the Todoist
  integration you configure yourself

## Project Status

This repository is an actively customized fork of Kvaesitso.
The current direction is focused on turning the launcher into a more intentional, focus-first Android experience.

The current feature branch is not release-ready: pinned-shortcut focus policy
([#98](https://github.com/siudajakub/focuslauncher/issues/98)) and focus metrics durability
([#100](https://github.com/siudajakub/focuslauncher/issues/100)) remain open, and the branch still
needs its device smoke pass. See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the verified snapshot.

## Installation

FocusLauncher is preparing its first public signed APK through GitHub Releases. As of 2026-07-16,
the repository has no published release; the Releases page will become the official channel after
the first tagged build passes the release gate. It is not published on Google Play or F-Droid.

You can also build it locally from source (see Development below).

## Development

### Requirements

- Android Studio
- Android SDK
- JDK 21 or newer

### Build

From the project root:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew :app:app:assembleDefaultDebug
```

If your local environment does not already expose the Android SDK path, create a `local.properties`
file with your `sdk.dir`. The APK is written to
`app/app/build/outputs/apk/default/debug/app-default-debug.apk`.

## Reporting Issues

If you notice a bug or regression, open an issue in this repository:
[github.com/siudajakub/focuslauncher/issues](https://github.com/siudajakub/focuslauncher/issues)

Please include:

- steps to reproduce
- device model and Android version
- logs, screenshots, or recordings if available
- any relevant launcher settings involved in the problem

## Contributing

Contributions are welcome.
If you want to work on a larger change, it is best to open an issue first so the direction can be discussed before implementation starts.

## Acknowledgements

FocusLauncher builds on top of the Kvaesitso codebase and the work of its contributors:

- [Kvaesitso](https://github.com/MM2-0/Kvaesitso)
- [Kvaesitso contributors](https://github.com/MM2-0/Kvaesitso/graphs/contributors)

## License

This project remains licensed under the GNU General Public License 3.0 unless noted otherwise.

```text
Copyright (C) 2021-2026 MM2-0, the Kvaesitso contributors, and FocusLauncher contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
```

`core/shared` remains licensed under the Apache License 2.0, as inherited from upstream.
