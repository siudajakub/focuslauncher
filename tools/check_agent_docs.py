#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REQUIRED_FILES = (
    "AGENTS.md",
    "CLAUDE.md",
    "PROJECT_STATUS.md",
    "ROADMAP.md",
    "CLEANUP_STATUS.md",
    "DESIGN_SYSTEM.md",
    "docs/engineering/architecture.md",
    "docs/engineering/focus-system.md",
    "docs/engineering/verification.md",
    "docs/engineering/code-review.md",
    "docs/engineering/work-management.md",
    "docs/sessions/README.md",
    "docs/sessions/TEMPLATE.md",
)
REQUIRED_TOOLS = (
    "tools/session_context.sh",
    "tools/session_new.sh",
)
SETTINGS_FILE = ".claude/settings.json"
REQUIRED_HOOK_EVENTS = ("SessionStart", "PreCompact")
STATUS_FILES = ("PROJECT_STATUS.md", "ROADMAP.md", "CLEANUP_STATUS.md")
EXPECTED_SKILLS = (
    "focus-feature-delivery",
    "launcher-launch-path-audit",
    "room-migration",
    "project-docs-sync",
    "pre-merge-review",
)
LINK_PATTERN = re.compile(r"(?<!!)\[[^]]+\]\(([^)]+)\)")


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def validate_markdown_links(path: Path, errors: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    for target in LINK_PATTERN.findall(text):
        if target.startswith(("http://", "https://", "#", "mailto:")):
            continue
        clean_target = target.split("#", 1)[0]
        if not clean_target:
            continue
        resolved = (path.parent / clean_target).resolve()
        if not resolved.exists():
            fail(errors, f"{path.relative_to(ROOT)}: broken local link: {target}")


def validate_skill(name: str, errors: list[str]) -> None:
    skill = ROOT / ".agents" / "skills" / name / "SKILL.md"
    metadata = skill.parent / "agents" / "openai.yaml"
    if not skill.is_file():
        fail(errors, f"missing skill: {skill.relative_to(ROOT)}")
        return
    if not metadata.is_file():
        fail(errors, f"missing skill metadata: {metadata.relative_to(ROOT)}")
    else:
        metadata_text = metadata.read_text(encoding="utf-8")
        if f"${name}" not in metadata_text:
            fail(errors, f"{metadata.relative_to(ROOT)}: default prompt must mention ${name}")
        if "TODO" in metadata_text:
            fail(errors, f"{metadata.relative_to(ROOT)}: unresolved template TODO")
    text = skill.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        fail(errors, f"{skill.relative_to(ROOT)}: missing YAML frontmatter")
    if f"name: {name}\n" not in text:
        fail(errors, f"{skill.relative_to(ROOT)}: name does not match directory")
    description = re.search(r"^description:\s*(.+)$", text, re.MULTILINE)
    if not description or "TODO" in description.group(1) or len(description.group(1)) < 40:
        fail(errors, f"{skill.relative_to(ROOT)}: description is missing or too vague")
    if "[TODO" in text:
        fail(errors, f"{skill.relative_to(ROOT)}: unresolved template TODO")


def validate_session_hooks(errors: list[str]) -> None:
    settings_path = ROOT / SETTINGS_FILE
    if not settings_path.is_file():
        fail(errors, f"missing {SETTINGS_FILE}: the SessionStart/PreCompact hooks live here")
        return
    try:
        settings = json.loads(settings_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(errors, f"{SETTINGS_FILE}: invalid JSON ({exc})")
        return
    hooks = settings.get("hooks", {})
    for event in REQUIRED_HOOK_EVENTS:
        groups = hooks.get(event)
        if not groups:
            fail(errors, f"{SETTINGS_FILE}: missing {event} hook")
            continue
        commands = [
            entry.get("command", "")
            for group in groups
            for entry in group.get("hooks", [])
        ]
        if not any("session_context.sh" in command for command in commands):
            fail(errors, f"{SETTINGS_FILE}: {event} hook must run tools/session_context.sh")


def main() -> int:
    errors: list[str] = []
    for relative in REQUIRED_FILES:
        path = ROOT / relative
        if not path.is_file():
            fail(errors, f"missing required document: {relative}")
            continue
        validate_markdown_links(path, errors)

    for relative in REQUIRED_TOOLS:
        if not (ROOT / relative).is_file():
            fail(errors, f"missing required tool: {relative}")

    validate_session_hooks(errors)

    agents = ROOT / "AGENTS.md"
    if agents.is_file() and len(agents.read_text(encoding="utf-8").splitlines()) > 140:
        fail(errors, "AGENTS.md exceeds the 140-line project limit")

    for relative in STATUS_FILES:
        path = ROOT / relative
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        if re.search(r"^- \[[ xX]\]", text, re.MULTILINE):
            fail(errors, f"{relative}: task checkbox found; use GitHub Issues")

    cleanup = ROOT / "CLEANUP_STATUS.md"
    if cleanup.is_file() and "FocusProfile` nadal istnieje" in cleanup.read_text(encoding="utf-8"):
        fail(errors, "CLEANUP_STATUS.md contains the obsolete FocusProfile claim")

    for skill_name in EXPECTED_SKILLS:
        validate_skill(skill_name, errors)

    if errors:
        print("Agent documentation validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Agent documentation validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
