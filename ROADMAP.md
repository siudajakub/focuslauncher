# FocusLauncher Roadmap

Last reviewed: 2026-06-14

## Product Goal

Build an Android launcher that helps people with attention difficulties use their phone deliberately through less browsing, clear essential-app access, explainable friction, and local-first focus support.

## Strategic Themes

### Stabilize The Focus Core

Maintain one coherent model for classification, temporary access, focus sessions, policy decisions, and history. Make every app-launch entry point behave consistently and recover safely after process death, reboot, or expired sessions.

### Reduce The Product Surface

Remove or isolate features that do not support the focus-first product. Distinguish clearly between a feature hidden from the UX and a module physically removed from the codebase.

### Make Support Explainable

Keep friction, limits, habits, environmental cues, and recommendations deterministic, local, reversible, and understandable from within the launcher.

### Prove Reliability

Expand JVM coverage for pure policy logic, maintain Room migration tests, build every pull request, and perform Pixel smoke tests for launcher lifecycle and launch interception changes.

### Prepare Distribution

Stabilize application identity, signing, privacy documentation, release notes, upgrade behavior, and reproducible release builds before public distribution.

## Work Tracking

Actionable tasks, priorities, owners, and completion state live in [GitHub Issues](https://github.com/siudajakub/focuslauncher/issues) and the repository project board. Architecture decisions and verified project state remain in the repository documentation.
