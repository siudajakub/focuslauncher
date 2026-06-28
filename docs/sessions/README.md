# Sessions

Shared, prunable working memory for sessions running on this repository. A *session* is one
agent or human work thread — a Claude tab, a Codex thread, a worktree, or a focused subagent.
The goal is to preserve context across context resets and to coordinate several sessions that
run at once without colliding.

This directory is **not** status, **not** a backlog, and **not** architecture:

- Verified facts about the tree → [`PROJECT_STATUS.md`](../../PROJECT_STATUS.md).
- Actionable tasks, priority, ownership → GitHub Issues (see [work-management](../engineering/work-management.md)).
- Durable rules and procedures → [`AGENTS.md`](../../AGENTS.md) and [`docs/engineering`](../engineering/).

A worklog here only captures *in-flight* context that does not yet belong in any of those.

## When To Open A Worklog

Open one whenever work will span more than a single sitting, may outlive a context window, or
runs alongside another session. For a quick, self-contained change you can skip it.

## Protocol

1. **Before starting**, list this directory. If an active worklog claims files or modules that
   overlap your task, coordinate or choose a non-overlapping slice. Parallel writers use
   **separate worktrees** and must not edit the same files concurrently
   (see [code-review.md](../engineering/code-review.md)).
2. **Scaffold the worklog** with `sh tools/session_new.sh <short-topic-slug>` — it copies
   [`TEMPLATE.md`](TEMPLATE.md) to `YYYY-MM-DD-<short-topic-slug>.md` with the date and branch
   prefilled. One file per session keeps parallel sessions from conflicting on the same file.
3. **Fill the claim**: goal, branch/worktree, and the files/modules you are touching. This is
   how other sessions see what is taken.
4. **Keep it current** as you work — decisions, current state, next step, verification status.
   Treat it as the handoff note you would want if you resumed cold.
5. **On completion**: move durable facts into `PROJECT_STATUS.md`, open or update GitHub Issues
   for follow-ups, then **delete the worklog** (or set Status to `DONE` if a near-term session
   will resume it). Do not let stale worklogs accumulate.

## Example

A filled worklog stays terse — enough for another session to pick up cold:

```markdown
# Session: focus gate copy pass

- Date: 2026-06-27
- Agent: Claude Code
- Branch / worktree: focus/gate-copy @ ../lanucher-gate-copy
- Status: ACTIVE

## Goal
Tighten the friction-gate explanation so blocked launches read calmly.

## Claim (files / modules being touched)
- `app/ui/.../launcher/focus/FocusLaunchCoordinator.kt`
- `core/i18n/src/main/res/values/strings.xml` (focus_gate_* keys)

## Current State
New strings drafted; coordinator wired to the calmer message. Dark-mode check pending.

## Next Step
Run `:app:ui:testDebugUnitTest`, then Pixel smoke for a distracting-app launch.
```

## Automation

For Claude Code, `SessionStart` and `PreCompact` hooks in `.claude/settings.json` run
`tools/session_context.sh` and inject the active worklogs (this directory, minus this README and
`TEMPLATE.md`) into context — so each session automatically sees other sessions' claims without
anyone remembering to look, and is reminded to flush its own worklog before context is compacted.
The hooks are read-only; opening, updating, and pruning worklogs is still a deliberate step you
take per this protocol. Codex relies on the same protocol via [`AGENTS.md`](../../AGENTS.md).

## Parallel Sessions Use Separate Worktrees

Two sessions editing the same checkout share one Git index and **will race** on `git add`/commit.
For genuine parallel work, give each session its own worktree — a separate working directory and
index on its own branch:

```bash
# from the main checkout: create a sibling worktree on a new branch
git worktree add ../lanucher-<topic> -b <branch>

# work in ../lanucher-<topic> from the parallel session, then when merged:
git worktree remove ../lanucher-<topic>
git worktree list   # show active worktrees
```

Keep worktrees as **siblings** of the repo, not inside it. Each worktree has its own
`.gradle-home`/build output, so set the build environment per worktree
(see [`AGENTS.md`](../../AGENTS.md) and [verification.md](../engineering/verification.md)). Record the
worktree path in the worklog's `Branch / worktree` line so other sessions can see where it lives.

## Conventions

- One worklog per session; never share a file between two concurrent sessions.
- Filename is date-prefixed and topic-slugged so `ls` reads as a chronological index.
- Keep entries terse and factual, matching the repository documentation voice.
- No task checkboxes that duplicate GitHub Issues — link the issue instead.
- Worklogs are committed so context survives across worktrees and machines; prune them when the
  work lands so the directory reflects only live sessions.
