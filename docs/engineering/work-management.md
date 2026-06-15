# Work Management

GitHub Issues and the repository project board are the source of truth for actionable work. Repository Markdown files record context, architecture, verified state, and operating rules.

Current board: https://github.com/users/siudajakub/projects/1

Seeded backlog range: https://github.com/siudajakub/focuslauncher/issues/1 through https://github.com/siudajakub/focuslauncher/issues/10

## Rules

- Do not add task checkboxes to `PROJECT_STATUS.md`, `ROADMAP.md`, or `CLEANUP_STATUS.md`.
- Create or update a GitHub Issue for every actionable product, cleanup, CI, release, documentation, or verification task.
- Keep `PROJECT_STATUS.md` factual and dated; update it only with verified state.
- Keep `ROADMAP.md` strategic; link to Issues for execution.
- Keep `CLEANUP_STATUS.md` as an inventory; link cleanup work to Issues with the `cleanup` label.

## Backlog Bootstrap

Run this after authenticating GitHub CLI:

```bash
python3 tools/create_github_backlog.py
```

The script is idempotent: it creates missing labels, reuses issues with matching titles, creates a `FocusLauncher Stabilization` project if needed, and adds the issues to that project.
