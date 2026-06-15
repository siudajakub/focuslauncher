---
name: project-docs-sync
description: Reconcile FocusLauncher agent and project documentation with the current code, Gradle graph, tests, verification evidence, and GitHub work tracking. Use after substantial implementation, cleanup, module removal, architecture changes, roadmap changes, or whenever project Markdown files contradict each other.
---

# Project Documentation Sync

1. Read `AGENTS.md`, `PROJECT_STATUS.md`, `ROADMAP.md`, `CLEANUP_STATUS.md`, and relevant `docs/engineering` files.
2. Verify claims against code, `settings.gradle.kts`, dependencies, routes, tests, migrations, and fresh command output. Do not infer physical removal from hidden UI.
3. Keep responsibilities separate:
   - `AGENTS.md`: universal Codex rules and routing.
   - `PROJECT_STATUS.md`: dated verified facts.
   - `ROADMAP.md`: strategic direction without task checkboxes.
   - `CLEANUP_STATUS.md`: factual legacy inventory without task checkboxes.
   - GitHub Issues/Project: actionable tasks and status.
4. Remove contradictions, vague completion claims, stale file paths, and duplicated procedures.
5. Open or update GitHub Issues for discovered work instead of adding Markdown TODO lists.
6. Run `python3 tools/check_agent_docs.py`.
7. Review the documentation diff independently from the implementation narrative.

Never record an unverified dirty-worktree feature as released or complete.
