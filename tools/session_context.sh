#!/bin/sh
# Emitted into Claude Code session context by the SessionStart hook in .claude/settings.json.
# Surfaces in-flight session worklogs so parallel sessions notice each other's file claims
# before editing. Read-only and side-effect free; always exits 0 so it never blocks a session.

dir="${CLAUDE_PROJECT_DIR:-.}/docs/sessions"
worklogs=$(ls "$dir"/*.md 2>/dev/null | grep -vE '/(README|TEMPLATE)\.md$' || true)

if [ -z "$worklogs" ]; then
  echo "Session context: no active worklogs in docs/sessions/."
  echo "If this work spans sessions or runs alongside another, copy docs/sessions/TEMPLATE.md"
  echo "to a dated worklog and record your file claims (see docs/sessions/README.md)."
  exit 0
fi

echo "Session context: active worklogs in docs/sessions/ — review for overlapping file"
echo "claims before editing the same files (separate worktrees for parallel writers)."
for f in $worklogs; do
  echo ""
  echo "### ${f#"${CLAUDE_PROJECT_DIR:-.}/"}"
  # First 40 lines carry the title, status, goal, and the file/module claim.
  awk 'NR<=40' "$f"
done
exit 0
