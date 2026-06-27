#!/bin/sh
# Emitted into Claude Code session context by the SessionStart hook in .claude/settings.json.
# Surfaces in-flight session worklogs so parallel sessions notice each other's file claims
# before editing. Read-only and side-effect free; always exits 0 so it never blocks a session.

root="${CLAUDE_PROJECT_DIR:-.}"
dir="$root/docs/sessions"
worklogs=$(ls "$dir"/*.md 2>/dev/null | grep -vE '/(README|TEMPLATE)\.md$' || true)

if [ -z "$worklogs" ]; then
  echo "Session context: no active worklogs in docs/sessions/."
  echo "If this work spans sessions or runs alongside another, scaffold one with"
  echo "  sh tools/session_new.sh <short-topic-slug>"
  echo "and record your file claims (see docs/sessions/README.md)."
  exit 0
fi

# Split worklogs into active (ACTIVE/BLOCKED) and DONE-but-not-yet-pruned.
active=""
stale=""
for f in $worklogs; do
  status=$(grep -m1 -iE '^[[:space:]-]*Status:' "$f" 2>/dev/null | tr '[:lower:]' '[:upper:]')
  case "$status" in
    *"|"*)  active="$active $f" ;;   # unfilled TEMPLATE placeholder (ACTIVE | BLOCKED | DONE)
    *DONE*) stale="$stale $f" ;;
    *)      active="$active $f" ;;
  esac
done

if [ -n "$active" ]; then
  echo "Session context: active worklogs in docs/sessions/ — review for overlapping file"
  echo "claims before editing the same files (use separate worktrees for parallel writers)."
  for f in $active; do
    echo ""
    echo "### ${f#"$root/"}"
    # First 40 lines carry the title, status, goal, and the file/module claim.
    awk 'NR<=40' "$f"
  done
fi

if [ -n "$stale" ]; then
  echo ""
  echo "Worklogs marked DONE — fold durable facts into PROJECT_STATUS.md, then delete:"
  for f in $stale; do echo "  - ${f#"$root/"}"; done
fi

exit 0
