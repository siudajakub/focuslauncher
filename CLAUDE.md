# CLAUDE.md

Entry point for Claude Code (web, CLI, and IDE) working in this repository.

The canonical, agent-agnostic rules live in **[AGENTS.md](AGENTS.md)** — read it first.
It applies to every agent, Codex and Claude alike. This file adds only what is
Claude- and multi-session-specific and deliberately does **not** duplicate AGENTS.md.
When a rule changes, edit AGENTS.md (the source of truth), not this file.

## Start Of Every Session

1. Read [AGENTS.md](AGENTS.md) — product framing, core rules, data rules, commands, definition of done.
2. Read [PROJECT_STATUS.md](PROJECT_STATUS.md) — latest verified snapshot of the tree.
3. Load the task-specific docs listed under "Context Loading" in AGENTS.md.
4. If other sessions are running in parallel, follow [docs/sessions/README.md](docs/sessions/README.md) before editing.

## Documentation Map

| Document | Purpose | Owns / update when |
| --- | --- | --- |
| [AGENTS.md](AGENTS.md) | Canonical agent rules, routing, commands, definition of done | Any change to how agents must work |
| [CLAUDE.md](CLAUDE.md) | Claude Code entry point + multi-session protocol | Claude- or session-workflow specifics only |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Dated, verified snapshot of current state | After verifying durable facts in the tree |
| [ROADMAP.md](ROADMAP.md) | Durable product direction (no task tracking) | Strategy shifts |
| [CLEANUP_STATUS.md](CLEANUP_STATUS.md) | Factual inventory of legacy surface (no task tracking) | Code evidence about legacy surface changes |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | UI patterns, theming, i18n rules | Before any UI work |
| [readme.md](readme.md) | Public-facing project overview | User-facing description changes |
| [docs/engineering/architecture.md](docs/engineering/architecture.md) | Module ownership and dependency boundaries | Architecture or module-boundary changes |
| [docs/engineering/focus-system.md](docs/engineering/focus-system.md) | Focus model, sources of truth, launch safety | Focus state, policy, visibility, launch changes |
| [docs/engineering/verification.md](docs/engineering/verification.md) | Build/test/smoke commands and the check matrix | Choosing checks or claiming completion |
| [docs/engineering/code-review.md](docs/engineering/code-review.md) | Writer/reviewer rule and merge gate | Reviewing or merging substantial changes |
| [docs/engineering/work-management.md](docs/engineering/work-management.md) | GitHub Issues / project board ownership | Backlog or task-tracking changes |
| [docs/sessions/README.md](docs/sessions/README.md) | Multi-session coordination protocol | Working alongside other sessions/worktrees |

`docs/superpowers/` holds historical plans and specs — context, not current status. GitHub
Issues and the project board (see [work-management](docs/engineering/work-management.md)) own actionable work.

**Tooling:** `tools/session_new.sh <slug>` scaffolds a worklog from the template;
`tools/session_context.sh` backs the `SessionStart`/`PreCompact` hooks; `tools/check_agent_docs.py`
validates the docs, the session scripts, and the hook wiring.

## Multi-Session Work

This project is built to support several sessions running at once (parallel worktrees,
separate Claude tabs, or subagents). To keep that safe and recoverable:

- **One worktree per concurrent writer.** Never edit the same files from two sessions at
  once — this matches the parallel-writer rule in [code-review.md](docs/engineering/code-review.md).
- **Each active session keeps a worklog** under [`docs/sessions/`](docs/sessions/README.md). It
  records the goal, branch/worktree, the files and modules it is claiming, key decisions,
  current state, and the next step — so a fresh session (after a context reset) or a
  parallel one can pick up without re-deriving everything.
- **Before starting, scan `docs/sessions/`** for overlapping scope and pick non-overlapping
  files, or coordinate.
- **On completion**, fold durable facts into `PROJECT_STATUS.md`, push actionable follow-ups
  to GitHub Issues, then prune the worklog. Worklogs are ephemeral working memory — not
  status, not a backlog, not architecture.

`SessionStart` and `PreCompact` hooks in `.claude/settings.json` run `tools/session_context.sh`
and inject the active worklogs into context automatically, so awareness of other sessions is not
left to memory; the `PreCompact` run also reminds you to flush your worklog before context is
summarized. Maintaining your own worklog stays a deliberate step. See
[docs/sessions/README.md](docs/sessions/README.md) for the full protocol and template.

## Claude Code Notes

- **Build / verify commands and the JDK 21 environment** live in
  [AGENTS.md](AGENTS.md) and [verification.md](docs/engineering/verification.md) — use those exact
  commands; do not invent new ones. Always set `GRADLE_USER_HOME="$PWD/.gradle-home"`.
- **Documentation changes** must keep `python3 tools/check_agent_docs.py` green; it validates the
  required docs, their internal links, AGENTS.md length, the `.agents` skills, the session scripts,
  and the `SessionStart`/`PreCompact` hook wiring.
- **Keep AGENTS.md ≤ 140 lines** (validator-enforced). Push detail into `docs/engineering/`.
- **Do not revert unrelated dirty-tree changes.** The working tree usually carries
  substantial uncommitted product work; integrate with it.
- **Match the existing voice** in docs: terse, declarative, English, no task checkboxes in the
  status/roadmap/cleanup files.
