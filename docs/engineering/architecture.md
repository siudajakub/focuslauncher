# Architecture

## Repository Shape

FocusLauncher is a multi-module Android application using Gradle Kotlin DSL, Jetpack Compose, Kotlin coroutines, Room, DataStore, and Koin.

- `app/app`: application bootstrap, packaging, variants, manifest, and `LauncherApplication`.
- `app/ui`: launcher activities, Compose surfaces, settings, search orchestration, and focus UI.
- `core`: shared search types, preferences, permissions, compatibility, profiles, i18n, and utilities.
- `data`: low-level repositories and providers for applications, custom attributes, database, files, widgets, and other data sources.
- `services`: higher-level APIs for search, icons, backup, favorites, widgets, and related orchestration.
- `libs`: standalone or vendored libraries.
- `plugins/sdk`: public plugin contracts retained from upstream.
- `docs`: VitePress user and contributor documentation.

The active module graph is defined only by `settings.gradle.kts`. Do not infer physical removal from a hidden settings route or deleted UI screen.

## Ownership Rules

- Persisted models and repositories belong in `core`, `data`, or `services`, not Compose screens.
- UI state derivation belongs in view models or pure model helpers near the owning feature.
- Cross-entry-point launch policy must be centralized and reused.
- Koin wiring belongs in module definitions or application bootstrap, following existing patterns.
- Prefer targeted extensions over restructuring upstream modules without a measured benefit.

## Persistent State

- DataStore model: `core/preferences/.../LauncherSettingsData.kt`.
- Settings wrappers: `core/preferences/.../ui` and related preference packages.
- Room database: `data/database/.../AppDatabase.kt`.
- Per-searchable attributes and temporary focus access: `data/customattrs`.
- Canonical English strings: `core/i18n/src/main/res/values/strings.xml`.

## Change Routing

- Launcher or search flow: start in `app/ui/.../launcher`, then trace service and data dependencies.
- Preferences: inspect `LauncherSettingsData`, wrapper APIs, serializers, and all consumers.
- Database: inspect entity, DAO, database version, migrations, schemas, and migration tests.
- UI: read `DESIGN_SYSTEM.md`, existing shared components, theme, and nearby screens first.
- Removed feature: check settings, Gradle graph, application wiring, routes, strings, serialization, migrations, and tests before deletion.

## Documentation Boundaries

- `AGENTS.md`: concise rules that apply to every agent task.
- `CLAUDE.md`: Claude Code entry point and multi-session protocol; defers shared rules to `AGENTS.md`.
- `PROJECT_STATUS.md`: dated, verified current state.
- `ROADMAP.md`: durable product direction, not task tracking.
- `CLEANUP_STATUS.md`: factual inventory, not task tracking.
- GitHub Issues/Project: actionable tasks, priority, ownership, and status.
- `docs/engineering`: architecture and procedures.
- `docs/sessions`: ephemeral in-flight worklogs for parallel sessions; not status or a backlog.
- `docs/superpowers`: historical plans and specifications; these are not current status.
