# FocusLauncher

FocusLauncher is an Android launcher built to reduce distractions and make intentional app use easier.
It is based on Kvaesitso, with the experience being adapted toward focus sessions, calmer browsing, and
stronger friction around distracting apps.

## What It Does

- keeps launcher search centered around apps
- adds focus-oriented settings and app classification
- lets you separate essential apps from distracting ones
- adds launch friction and session-based unlocking for distracting apps
- aims for a cleaner, less impulse-driven home experience

## Project Status

This repository is an actively customized fork of Kvaesitso.
The current direction is focused on turning the launcher into a more intentional, focus-first Android experience.

## Installation

There is no public distribution channel configured yet.
For now, the project is intended to be built locally from source or shared through GitHub releases later.

## Development

### Requirements

- Android Studio
- Android SDK
- JDK 21 or newer

### Build

From the project root:

```bash
./gradlew assembleDebug
```

If your local environment does not already expose the Android SDK path, create a `local.properties`
file with your `sdk.dir`.

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

The plugin SDK modules (`plugins/sdk` and `core/shared`) remain licensed under the Apache License 2.0.
