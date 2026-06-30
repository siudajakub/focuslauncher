# Changelog

## 1.0.0 — first public release

First public build of FocusLauncher, a focus-first Android launcher forked from Kvaesitso and
reduced to a deliberate, apps-first product.

- **Focus core:** app classification (essential vs distracting), focus sessions with temporary
  unlocks, explainable launch friction, daily limits, focus history, and weekly focus insights.
- **Time Awareness:** optional periodic time reminders, on a safe foreground-service path.
- **Quick Capture:** lossless local notes with optional share.
- **Apps-first search:** local app and pinned-shortcut search; the general-purpose web/Wikipedia,
  calculator, contacts, files, locations, and cloud search providers from upstream are removed.
- **Settings:** focus settings consolidated into a single hub.
- **Privacy:** local-first; the only network egress is from optional features you enable (Weather,
  and any integration you connect). See `docs/privacy-policy.md`.

Distributed as a signed APK via GitHub Releases. Built on JDK 21.
