#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import subprocess
import sys
from dataclasses import dataclass


PROJECT_TITLE = "FocusLauncher Stabilization"


@dataclass(frozen=True)
class IssueSpec:
    title: str
    labels: tuple[str, ...]
    body: str


LABELS = {
    "focus": ("7057ff", "Focus system, policy, session, and launcher behavior"),
    "cleanup": ("8a63d2", "Legacy surface or code removal"),
    "docs": ("0e8a16", "Documentation and agent guidance"),
    "ci": ("1d76db", "Continuous integration and automation"),
    "verification": ("fbca04", "Testing, smoke tests, and release confidence"),
    "migration": ("d73a4a", "Database, DataStore, or compatibility migration"),
    "product": ("0052cc", "Product direction and user-facing decisions"),
}


ISSUES = (
    IssueSpec(
        title="Stabilize focus session runtime and expiry recovery",
        labels=("focus", "verification"),
        body="""## Goal
Make focus-session start, expiry, reconciliation, DND restoration, and process-restart behavior reliable.

## Acceptance criteria
- Session start, manual end, scheduled expiry, stale worker, and launcher restart are covered by tests or a documented smoke test.
- `FocusSessionRuntime`, `FocusSessionRepository`, scheduler, and `FocusPolicyService` agree on the active session projection.
- `PROJECT_STATUS.md` is updated only after fresh verification.
""",
    ),
    IssueSpec(
        title="Audit all launcher app launch paths for focus-policy consistency",
        labels=("focus", "verification"),
        body="""## Goal
Ensure every app launch or visibility entry point uses the same classification and policy decision.

## Acceptance criteria
- Search tap, best-match launch, home essentials, hidden/settings flows, customization surfaces, and gate continuation are audited.
- Bypasses are removed or explicitly documented as intentional exceptions.
- A regression test or review matrix is added for the affected logic.
""",
    ),
    IssueSpec(
        title="Complete physical cleanup for removed online search providers",
        labels=("cleanup", "migration"),
        body="""## Goal
Finish cleanup for calculator, websites, Wikipedia, Nextcloud, and Owncloud after their removal from the active Gradle graph.

## Acceptance criteria
- Dead routes, imports, preferences, serializers, strings, icons, and docs are removed or intentionally retained with a compatibility note.
- Compatibility data migrations are reviewed before deleting persisted fields.
- Build and relevant tests pass.
""",
    ),
    IssueSpec(
        title="Decide retained integrations for the focus-first product",
        labels=("product", "cleanup"),
        body="""## Goal
Make a product decision for weather, plugins, feed, music, unit conversion, widgets, calendar, contacts, files, and locations.

## Acceptance criteria
- Each subsystem is classified as core, advanced-only, developer-only, or removal candidate.
- Decisions are recorded in an ADR or product note.
- Follow-up cleanup issues exist for every removal candidate.
""",
    ),
    IssueSpec(
        title="Remove or migrate legacy search and provider preferences",
        labels=("cleanup", "migration"),
        body="""## Goal
Eliminate stale preference fields for removed search providers without breaking existing installs.

## Acceptance criteria
- `LauncherSettingsData` and preference wrappers are audited for removed-provider fields.
- Necessary DataStore or compatibility migration behavior is documented and tested.
- No active UI exposes removed-provider settings.
""",
    ),
    IssueSpec(
        title="Export Room schema 36 and harden migration coverage",
        labels=("migration", "verification"),
        body="""## Goal
Make the `35 -> 36` focus cleanup migration fully auditable.

## Acceptance criteria
- Schema `36.json` is exported and committed.
- Migration tests cover legacy focus payload removal and `FocusTemporaryUnlock` preservation.
- CI migration job passes on an emulator.
""",
    ),
    IssueSpec(
        title="Create Pixel 8 focus launcher smoke-test checklist",
        labels=("verification", "focus"),
        body="""## Goal
Document repeatable manual validation for launcher startup, focus gate, focus session, app launch, and crash recovery on Pixel 8.

## Acceptance criteria
- Checklist covers install, launch, essential app, distracting app, temporary unlock, active focus session, time reminders, and crash diagnostics.
- Commands are aligned with `docs/engineering/verification.md`.
- The checklist is referenced from PR or release validation.
""",
    ),
    IssueSpec(
        title="Clean up focus support copy and localization consistency",
        labels=("focus", "docs"),
        body="""## Goal
Make focus support copy calm, consistent, and fully localized through canonical English strings.

## Acceptance criteria
- No hardcoded user-facing Compose strings remain in touched focus surfaces.
- Mixed Polish/English strings are reviewed and normalized in `strings.xml`.
- Copy follows the design system's low-distraction tone.
""",
    ),
    IssueSpec(
        title="Prepare CI and release artifact policy for public distribution",
        labels=("ci", "verification"),
        body="""## Goal
Make builds, nightly artifacts, signing expectations, and release checks explicit before public distribution.

## Acceptance criteria
- CI covers docs, JVM tests, debug build, and migration tests.
- Nightly signing behavior and secret requirements are documented.
- Release readiness includes a final fresh-context review and Pixel smoke test.
""",
    ),
    IssueSpec(
        title="Keep project documentation synchronized with code and issues",
        labels=("docs", "ci"),
        body="""## Goal
Prevent future drift between `AGENTS.md`, status docs, roadmap, cleanup inventory, and actual code.

## Acceptance criteria
- `tools/check_agent_docs.py` runs in CI.
- Actionable Markdown checkboxes are rejected in status files.
- Documentation updates link to GitHub Issues for execution tasks.
""",
    ),
)


def run(args: list[str], *, capture: bool = True) -> str:
    result = subprocess.run(args, text=True, capture_output=capture, check=False)
    if result.returncode != 0:
        message = result.stderr.strip() or result.stdout.strip()
        raise RuntimeError(f"command failed: {' '.join(args)}\n{message}")
    return result.stdout.strip() if capture else ""


def gh_json(args: list[str]) -> object:
    output = run(["gh", *args])
    return json.loads(output) if output else None


def expected_repo_from_git() -> str:
    remote = run(["git", "config", "--get", "remote.origin.url"])
    match = re.search(r"github\.com[:/]([^/]+/[^/]+?)(?:\.git)?$", remote)
    if not match:
        raise RuntimeError(f"could not determine GitHub repository from remote.origin.url: {remote}")
    return match.group(1)


def repo_info() -> tuple[str, str, str]:
    expected_repo = expected_repo_from_git()
    info = gh_json(["repo", "view", expected_repo, "--json", "nameWithOwner,owner"])
    name_with_owner = info["nameWithOwner"]
    if name_with_owner.lower() != expected_repo.lower():
        raise RuntimeError(f"gh resolved {name_with_owner}, expected {expected_repo}")
    owner = info["owner"]["login"]
    return name_with_owner, owner, name_with_owner.split("/", 1)[1]


def ensure_labels(repo: str) -> None:
    existing = {
        label["name"]
        for label in gh_json(["label", "list", "--repo", repo, "--limit", "200", "--json", "name"])
    }
    for name, (color, description) in LABELS.items():
        if name in existing:
            continue
        run(["gh", "label", "create", name, "--repo", repo, "--color", color, "--description", description])


def find_issue(repo: str, title: str) -> dict[str, object] | None:
    query = f'in:title "{title}"'
    matches = gh_json([
        "issue",
        "list",
        "--repo",
        repo,
        "--state",
        "all",
        "--search",
        query,
        "--json",
        "number,title,url",
        "--limit",
        "20",
    ])
    for issue in matches:
        if issue["title"] == title:
            return issue
    return None


def ensure_issue(repo: str, spec: IssueSpec) -> dict[str, object]:
    existing = find_issue(repo, spec.title)
    if existing:
        print(f"issue exists: #{existing['number']} {spec.title}")
        return existing
    url = run([
        "gh",
        "issue",
        "create",
        "--repo",
        repo,
        "--title",
        spec.title,
        "--body",
        spec.body,
        *sum((["--label", label] for label in spec.labels), []),
    ])
    output = gh_json(["issue", "view", url, "--repo", repo, "--json", "number,title,url"])
    print(f"issue created: #{output['number']} {spec.title}")
    return output


def ensure_project(owner: str) -> dict[str, object]:
    projects = gh_json(["project", "list", "--owner", owner, "--format", "json", "--limit", "100"])
    for project in projects.get("projects", []):
        if project.get("title") == PROJECT_TITLE:
            print(f"project exists: {PROJECT_TITLE}")
            return project
    project = gh_json(["project", "create", "--owner", owner, "--title", PROJECT_TITLE, "--format", "json"])
    print(f"project created: {PROJECT_TITLE}")
    return project


def add_to_project(owner: str, project: dict[str, object], issue_url: str) -> None:
    number = str(project.get("number") or project.get("projectNumber") or "")
    if not number:
        raise RuntimeError("project number unavailable; skipping project item add")
    try:
        run(["gh", "project", "item-add", number, "--owner", owner, "--url", issue_url])
    except RuntimeError as error:
        if "already exists" not in str(error).lower():
            raise


def main() -> int:
    try:
        run(["gh", "auth", "status"])
        repo, owner, _ = repo_info()
        ensure_labels(repo)
        issues = [ensure_issue(repo, spec) for spec in ISSUES]
        project = ensure_project(owner)
        for issue in issues:
            add_to_project(owner, project, issue["url"])
        print("GitHub backlog sync complete.")
        return 0
    except Exception as error:
        print(error, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
