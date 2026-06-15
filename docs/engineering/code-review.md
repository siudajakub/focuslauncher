# Code Review

## Writer And Reviewer Rule

Substantial changes require an independent review in fresh context before merge. A change is substantial when it affects focus policy, launch routing, persistence, migrations, shared preferences, application startup, multiple modules, or a broad UI flow.

The reviewer must not rely on the writer's reasoning transcript. Give the reviewer:

- The goal and acceptance criteria.
- The final diff or branch comparison.
- Relevant architecture and verification documents.
- Test output or explicit missing checks.

Ask for bugs, regressions, missing requirements, inconsistent entry points, migration risks, and test gaps. Do not ask for general style commentary.

## Codex Workflow

Preferred order:

1. Complete implementation and local verification in the writer thread.
2. Start a new Codex thread, worktree, or explicitly requested fresh-context subagent.
3. Run review against the base branch or uncommitted diff.
4. Return findings with file and line references.
5. Address valid findings in the writer thread.
6. Re-run affected checks and inspect the final diff.

Use the repository skill `$pre-merge-review` for the review contract. Parallel writers must use separate worktrees and must not edit the same files concurrently.

## Merge Gate

- No unresolved high-severity findings.
- Acceptance criteria checked against the final diff.
- Required CI checks green.
- Database and preference compatibility reviewed when applicable.
- Documentation reflects the final behavior.
