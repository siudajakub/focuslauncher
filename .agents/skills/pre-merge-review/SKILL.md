---
name: pre-merge-review
description: Perform an independent fresh-context review of a substantial FocusLauncher diff before merge. Use for focus policy, launch routing, persistence, migration, application startup, shared preference, multi-module, or broad UI changes, and whenever the user asks whether a branch is ready to merge.
---

# Pre-Merge Review

Run this review in a new Codex thread, isolated reviewer worktree, or explicitly requested fresh-context subagent. Do not reuse the writer's reasoning as evidence.

1. Read `AGENTS.md`, `docs/engineering/code-review.md`, the task acceptance criteria, and the final diff against the intended base.
2. Inspect affected architecture and all callers, not only edited files.
3. Prioritize behavioral bugs, regressions, inconsistent launch paths, lifecycle races, data loss, migration defects, security/privacy issues, and missing tests.
4. Check every acceptance criterion against code and evidence.
5. Verify that reported test commands match the risk of the change. Run additional focused checks when feasible.
6. Return findings first, ordered by severity, with precise file and line references. Avoid style-only feedback.
7. If no findings remain, state residual risks and any checks that were not run.

The writer must address valid findings and rerun affected checks before merge.
