---
name: launcher-launch-path-audit
description: Audit FocusLauncher app launch, visibility, and focus-policy entry points for behavioral consistency. Use when changing app classification, search filtering, hidden apps, best-match launch, focus gate behavior, temporary unlocks, home essentials, shortcuts, or any code that opens an app.
---

# Launcher Launch Path Audit

1. Read `docs/engineering/focus-system.md`.
2. Identify the shared classification and policy APIs used by the change.
3. Search for application launch calls, launch coordinators, searchable click actions, best-match handling, home app actions, hidden-item flows, customization actions, and gate continuation.
4. Build a compact matrix with each entry point and expected behavior for essential, distracting, unclassified, temporarily unlocked, session-locked, and budget-blocked apps.
5. Report any path that bypasses shared policy, uses a different app key, or applies visibility differently.
6. Fix inconsistencies when implementation was requested; otherwise return findings with file and line references.
7. Add regression tests around pure routing or decision logic where possible.
8. Run relevant unit tests and the debug build when code changed.

Treat direct launches from settings and hidden-item surfaces as intentional exceptions only when the product requirement explicitly says so and the exception is documented.
