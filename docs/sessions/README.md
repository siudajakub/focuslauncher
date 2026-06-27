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
2. **Copy [`TEMPLATE.md`](TEMPLATE.md)** to `YYYY-MM-DD-<short-topic-slug>.md`
   (e.g. `2026-06-27-focus-gate-copy.md`). One file per session keeps parallel sessions from
   conflicting on the same file. Use today's date.
3. **Fill the claim**: goal, branch/worktree, and the files/modules you are touching. This is
   how other sessions see what is taken.
4. **Keep it current** as you work — decisions, current state, next step, verification status.
   Treat it as the handoff note you would want if you resumed cold.
5. **On completion**: move durable facts into `PROJECT_STATUS.md`, open or update GitHub Issues
   for follow-ups, then **delete the worklog** (or set Status to `DONE` if a near-term session
   will resume it). Do not let stale worklogs accumulate.

## Conventions

- One worklog per session; never share a file between two concurrent sessions.
- Filename is date-prefixed and topic-slugged so `ls` reads as a chronological index.
- Keep entries terse and factual, matching the repository documentation voice.
- No task checkboxes that duplicate GitHub Issues — link the issue instead.
- Worklogs are committed so context survives across worktrees and machines; prune them when the
  work lands so the directory reflects only live sessions.
