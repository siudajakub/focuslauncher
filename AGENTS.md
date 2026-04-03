# AGENTS.md

## Project Intent
- This repository is a fork of `Kvaesitso`, an open source Android launcher centered around search-first navigation.
- This fork is intentionally moving toward a `minimal`, `focus-first`, `anti-distraction` product direction.
- Preserve upstream architecture where practical. Prefer targeted extensions over large rewrites.
- Optimize for:
  - less browsing
  - calmer launcher surfaces
  - deliberate app launches
  - strong defaults over excessive configuration

## Repo Shape
- Main repo root is this directory.
- The project is a multi-module Android app built with `Gradle Kotlin DSL`.
- Primary module groups from `settings.gradle.kts`:
  - `:app`
    - `:app:app`: thin application module, `LauncherApplication`, packaging and variants
    - `:app:ui`: almost all UI, Jetpack Compose, activities, screens, launcher surface
  - `:core`
    - shared types, preferences, permissions, i18n, profiles, compatibility, device pose
  - `:data`
    - low-level repositories and data sources for apps, files, widgets, weather, wikipedia, database, custom attributes, plugins, searchables
  - `:services`
    - higher-level business APIs for search, icons, tags, widgets, favorites, backup, music, plugins, badges, accounts
  - `:libs`
    - standalone libraries and vendored integrations such as Nextcloud, Owncloud, WebDAV, material-color utilities
  - `:plugins:sdk`
    - public plugin SDK
  - `docs`
    - VitePress documentation site

## Architecture Landmarks
- Dependency injection uses `Koin`.
  - Most modules expose a `Module.kt` with DI bindings.
  - Application wiring lives in `app/app/src/main/java/de/mm20/launcher2/LauncherApplication.kt`.
- UI is heavily concentrated in `app/ui`.
  - Launcher flow: `ui/launcher`
  - Settings flow: `ui/settings`
  - Shared composables: `ui/component`, `ui/base`, `ui/common`
  - Theme system: `ui/theme`
- Persistent app data is split by concern:
  - Room database in `data/database`
  - app/user preferences in `core/preferences`
  - per-item customization in `data/customattrs`
  - localized strings in `core/i18n`
- Search is cross-cutting.
  - UI orchestration lives under `app/ui/.../launcher/search`
  - service APIs live in `services/search`
  - data providers live across `data/*`

## Current Fork-Specific Focus
- This fork already contains in-progress focus-mode work. Treat that as the active product direction.
- The single source of truth for per-app focus behavior is:
  - `data/customattrs/src/main/java/de/mm20/launcher2/data/customattrs/FocusProfile.kt`
- Focus-related persistence and preferences currently touch:
  - `data/customattrs/src/main/java/de/mm20/launcher2/data/customattrs/CustomAttributesRepository.kt`
  - `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
  - `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/FocusSettings.kt`
  - `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/SearchUiSettings.kt`
  - `core/preferences/src/main/java/de/mm20/launcher2/preferences/ui/UiSettings.kt`
- Focus UI and launch interception currently touch:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/focus/`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/scaffold/FocusHomeComponent.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/SharedLauncherActivity.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/LauncherScaffoldVM.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/SearchVM.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/sheets/CustomizeSearchableSheet.kt`
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/sheets/CustomizeSearchableSheetVM.kt`
- Do not introduce a second focus-state model unless there is no viable alternative.

## Working Rules
- Prefer small, behavior-preserving diffs.
- Respect the existing module split. If logic belongs in `data` or `services`, do not bury it in `app/ui`.
- Reuse existing repositories, settings wrappers and Koin modules instead of adding parallel state flows.
- When editing localized copy, update canonical English strings in:
  - `core/i18n/src/main/res/values/strings.xml`
- When changing preferences:
  - wire through `LauncherSettingsData`
  - expose the setting in the relevant `core/preferences/...` wrapper
  - update migrations only when persistence format actually changes
- When changing database-backed models:
  - inspect `data/database/AppDatabase.kt`
  - add/update DAO, entity and migration together
- Be careful with Compose `viewModel()` usage:
  - classes instantiated that way must not be `private`

## Launcher And Search Safety Checks
- Launch behavior has multiple entry points. Keep them aligned when changing app launch or app visibility rules:
  - tap from search results
  - enter / best-match launch
  - hidden-items and settings launch flows
  - home or essentials launch surfaces
  - item customization sheets
- Search changes can affect ranking, visibility, filters and actions simultaneously.
  - audit `SearchVM`, search item view-models, filter definitions and relevant preferences before changing ranking or hiding logic

## Where To Look First
- App bootstrap:
  - `app/app/src/main/java/de/mm20/launcher2/LauncherApplication.kt`
- Main launcher UI:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/`
- Settings:
  - `app/ui/src/main/java/de/mm20/launcher2/ui/settings/`
- Preferences and DataStore:
  - `core/preferences/src/main/java/de/mm20/launcher2/preferences/`
- Search filters and base search types:
  - `core/base/src/main/java/de/mm20/launcher2/search/`
- Per-item customization:
  - `data/customattrs/src/main/java/de/mm20/launcher2/data/customattrs/`
- Room database:
  - `data/database/src/main/java/de/mm20/launcher2/database/`
- Icons:
  - `services/icons/src/main/java/de/mm20/launcher2/icons/`
- Plugin framework:
  - `services/plugins/`
  - `data/plugins/`
  - `plugins/sdk/`
- Developer docs:
  - `docs/docs/developer-guide/`

## Build And Verification
- Main app module: `:app:app`
- Preferred debug build:
```bash
ANDROID_HOME=$HOME/Library/Android/sdk \
ANDROID_SDK_ROOT=$HOME/Library/Android/sdk \
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home \
./gradlew :app:app:assembleDebug
```
- Other useful builds:
```bash
ANDROID_HOME=$HOME/Library/Android/sdk \
ANDROID_SDK_ROOT=$HOME/Library/Android/sdk \
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home \
./gradlew :app:app:assembleDefaultDebug
```
```bash
ANDROID_HOME=$HOME/Library/Android/sdk \
ANDROID_SDK_ROOT=$HOME/Library/Android/sdk \
GRADLE_USER_HOME=/Users/j/vibe/lanucher/kvaesitso/.gradle-home \
./gradlew test
```
- Default debug APK path:
  - `app/app/build/outputs/apk/default/debug/app-default-debug.apk`

## Tests
- Test coverage is light. Existing JVM tests are in:
  - `core/base/src/test`
  - `core/ktx/src/test`
  - `data/locations/src/test`
- For launcher or focus-flow work, expect most validation to be manual unless you add new tests.

## Docs
- Documentation site is separate from the Android app and lives in `docs/`.
- VitePress scripts:
```bash
cd docs
npm install
npm run docs:dev
```
- Update docs when changing user-visible settings, plugin APIs, or contributor-facing architecture.

## Device Testing
- Preferred target device is a `Google Pixel 8`.
- Useful commands:
```bash
adb devices -l
adb install -r app/app/build/outputs/apk/default/debug/app-default-debug.apk
adb shell am start -W -n de.mm20.launcher2.debug/de.mm20.launcher2.ui.launcher.LauncherActivity
adb shell dumpsys activity exit-info de.mm20.launcher2.debug
adb shell dumpsys dropbox --print YYYY-MM-DD HH:MM:SS data_app_crash
```
- If startup crashes are hard to catch in `logcat`, prefer:
  - `dumpsys activity exit-info`
  - `dumpsys dropbox --print ... data_app_crash`

## Local Workspace Notes
- The worktree may be dirty. Do not revert unrelated user changes.
- `.gradle-home/` is local build state, not product code.
- There is active work around focus mode and launcher behavior; read nearby modified files before making assumptions.

## What Good Changes Look Like Here
- Better than upstream for this fork means:
  - less temptation
  - less visual noise
  - fewer accidental launches
  - faster access to essentials
  - predictable behavior on Pixel hardware
- A good change usually:
  - keeps architecture recognizable
  - reuses existing settings and repositories
  - preserves search/launch consistency
  - updates strings and docs when behavior changes
