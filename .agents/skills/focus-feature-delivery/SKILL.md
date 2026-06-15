---
name: focus-feature-delivery
description: Deliver a FocusLauncher feature end to end across product behavior, architecture, UI, persistence, tests, documentation, and verification. Use for new focus features or substantial changes to focus settings, sessions, policy, home, gate, reports, or attention-support behavior.
---

# Focus Feature Delivery

1. Read `AGENTS.md`, `docs/engineering/focus-system.md`, `docs/engineering/architecture.md`, and `DESIGN_SYSTEM.md` for UI work.
2. Trace the existing state flow from persistence through policy and every affected UI entry point. Reuse the current classification, session, and temporary-unlock models.
3. Write acceptance criteria covering normal behavior, recovery, process restart, expired state, and essential-versus-distracting behavior.
4. Implement the smallest cohesive feature slice. Keep pure policy decisions testable outside Android components.
5. Audit search tap, best match, home, hidden/settings flows, customization, and gate continuation when launch behavior changes. Invoke `$launcher-launch-path-audit` for broad changes.
6. Add focused tests. Use `$room-migration` when persistence schema changes.
7. Run the checks selected from `docs/engineering/verification.md` and inspect the final diff.
8. Update durable docs only when architecture, verified state, or user-visible behavior changed. Track remaining work in GitHub Issues.
9. For substantial work, invoke `$pre-merge-review` in fresh context before merge.

Do not create parallel focus-state models, hide missing integration behind UI-only checks, or claim completion without fresh verification output.
