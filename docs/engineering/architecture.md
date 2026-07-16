# Architecture

## Repository Shape

FocusLauncher is a multi-module Android application using Gradle Kotlin DSL, Jetpack Compose, Kotlin coroutines, Room, DataStore, and Koin.

- `app/app`: application bootstrap, packaging, variants, manifest, and `LauncherApplication`.
- `app/ui`: launcher activities, Compose surfaces, settings, search orchestration, and focus UI.
- `core`: shared search types, preferences, permissions, compatibility, profiles, i18n, and utilities.
- `data`: low-level repositories and providers for applications, shortcuts, calendar, custom
  attributes, Room, themes, widgets, and notifications.
- `services`: higher-level APIs for search, icons, backup, favorites, focus, widgets, music,
  tags, badges, and global actions. `services/focus` owns focus classification, policy,
  launch coordination, sessions, history, usage-summary models, UsageStats access, and
  session-expiry scheduling; Android activities, Compose screens, and the gate-launcher
  implementation stay in `app/ui`.
- `libs`: the vendored address formatter and Material color utilities.
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

## Current Product Boundaries

- Launcher search returns installed applications, plus pinned shortcut favorites matched by label —
  the set through which a PWA added to the home screen surfaces. There is no other result type and
  no filter model: `SearchService` takes one application repository. The code labels that second set
  as PWAs but does not reliably distinguish web shortcuts from native/legacy shortcuts; this is a
  known launch-policy gap.
- Calendar, widgets, and music remain core because they support the focus home and planning flows.
- The launcher performs no periodic background work and no network or location access of its own.
  Adding either needs an explicit product decision, not just a module.
- Weather, currency/unit conversion, the plugin system, device pose, feed, contacts,
  files/WebDAV/accounts, locations, calculator, website search, and Wikipedia have been physically
  removed. See [integrations-decision.md](integrations-decision.md).

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
