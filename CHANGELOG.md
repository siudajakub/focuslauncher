# Changelog

## 1.0.0 — unreleased

Candidate scope for the first public FocusLauncher build. No `1.0.0` GitHub Release has been
published yet.

- **Focus core:** app classification (essential vs distracting), focus sessions with temporary
  unlocks, explainable launch friction, daily limits, focus history, and weekly focus insights.
- **Intentional-use feedback:** gate turn-away reinforcement, per-app open counts, and optional
  on-device foreground-time reflection for distracting apps.
- **Quick Capture:** lossless local notes with optional share.
- **Apps-first search:** search returns installed apps and the web apps you pinned to the home
  screen — nothing else. Every other upstream provider (web, Wikipedia, calculator, contacts,
  files, locations, cloud, unit and currency conversion) is gone.
- **Settings:** focus settings consolidated into a single hub.
- **Privacy:** local-first. The launcher runs no background service, asks for no location, and makes
  no automatic network requests; the only outbound traffic is the Todoist integration, and only once
  you configure a token. See `docs/privacy-policy.md`.
- **Removed from upstream Kvaesitso:** weather and its widget, the currency and unit converters,
  the entire plugin system and its SDK, and device pose.
- **Removed after review:** grayscale (launcher and system-wide) — the launcher no longer touches
  display saturation, which is the user's setting to make; and Time Awareness reminders, whose
  always-on foreground service was the app's largest battery cost for a nudge a launcher cannot
  enforce.

Known pre-release blockers: focus-policy routing for pinned shortcuts (#98) and durable/correct
focus metrics (#100).

The release workflow targets a signed APK on GitHub Releases and builds with JDK 21.
