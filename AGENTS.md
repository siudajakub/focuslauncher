# FocusLauncher Agent Guide

## Product

FocusLauncher is a fork of Kvaesitso moving toward a minimal, focus-first Android launcher.
Prefer calmer surfaces, deliberate app launches, strong defaults, and targeted changes that
preserve the upstream architecture.

## Read First

- [Architecture](docs/engineering/architecture.md) for module ownership and dependency boundaries.
- [Focus system](docs/engineering/focus-system.md) before changing focus state, policy, search visibility, or launch behavior.
- [Design system](DESIGN_SYSTEM.md) before any UI work.
- [Verification](docs/engineering/verification.md) before selecting checks or claiming completion.
- [Code review](docs/engineering/code-review.md) before merging a substantial change.
- [Work management](docs/engineering/work-management.md) before adding or changing task tracking.
- [Project status](PROJECT_STATUS.md) for the latest verified snapshot.
- [Roadmap](ROADMAP.md) for product direction. GitHub Issues and the project board own actionable work.
- [Sessions protocol](docs/sessions/README.md) before running alongside other sessions or worktrees. Claude Code starts from [CLAUDE.md](CLAUDE.md).

## Context Loading

Always read this file first. Then list `docs/sessions/` and read any worklog whose claim
overlaps your task, so you do not collide with a parallel session.

Before implementation, read the linked project docs that match the task:

- Architecture or module-boundary changes: `docs/engineering/architecture.md`.
- Focus, search, app visibility, launch behavior, or session changes: `docs/engineering/focus-system.md`.
- UI changes: `DESIGN_SYSTEM.md`.
- Build, test, CI, release, migration, or verification changes: `docs/engineering/verification.md`.
- GitHub Issues, project board, backlog, roadmap, or status changes: `docs/engineering/work-management.md`.
- Large changes before merge: `docs/engineering/code-review.md`.

For broad, unclear, or cross-cutting tasks, read every file in `docs/engineering/` before editing.

## Core Rules

- Preserve module boundaries; do not bury data or service logic in `app/ui`.
- Reuse existing Koin modules, repositories, settings wrappers, and state flows.
- Do not introduce a second focus-state model.
- Keep all app-launch entry points consistent when changing focus policy or visibility.
- Update canonical English copy in `core/i18n/src/main/res/values/strings.xml`.
- Do not hardcode user-facing Compose strings or colors.
- Classes instantiated with Compose `viewModel()` must not be `private`.
- Work with existing dirty-tree changes; never revert unrelated user work.

## Data Changes

- Preferences: update `LauncherSettingsData` and the relevant wrapper under `core/preferences`.
- Room: update entity, DAO, database version, migration, exported schema, and migration tests together.
- Focus classification is stored in `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Temporary app access uses `FocusTemporaryUnlock`; active focus sessions use the session repository/runtime.

## Commands

Use the repository-local Gradle home:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.gradle-home"
```

Use JDK 21 or newer for full Gradle verification.

Fast checks:

```bash
./gradlew :app:ui:testDebugUnitTest
./gradlew :app:app:assembleDefaultDebug
python3 tools/check_agent_docs.py
```

Full checks:

```bash
./gradlew test :app:app:assembleDefaultDebug
./gradlew :data:database:connectedDebugAndroidTest
```

Default APK: `app/app/build/outputs/apk/default/debug/app-default-debug.apk`.

## Definition Of Done

- The requested behavior works across every affected entry point.
- Relevant tests are added or updated and fresh checks pass.
- User-visible behavior, architecture, and project status docs are updated when affected.
- The final diff contains no unrelated churn, debug artifacts, or stale generated files.
- Substantial changes receive a fresh-context review following `docs/engineering/code-review.md`.

## Parallel Sessions

- One worktree per concurrent writer; never edit the same files from two sessions at once.
- Keep a worklog under `docs/sessions/` for work that spans sittings or runs in parallel; scan that directory for overlapping claims before starting.
- Fold durable facts into `PROJECT_STATUS.md` and follow-ups into GitHub Issues, then prune the worklog. See [docs/sessions/README.md](docs/sessions/README.md).

## Working Mode

- During planning, clarify product intent and major tradeoffs.
- During execution, deliver the complete feature slice, including adjacent integration and verification.
- Pause only for destructive work or decisions with materially different product consequences.
