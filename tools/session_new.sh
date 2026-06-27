#!/bin/sh
# Scaffold a session worklog from docs/sessions/TEMPLATE.md with the date and branch prefilled.
# Usage: sh tools/session_new.sh <short-topic-slug>
# This is a convenience for the deliberate worklog step in docs/sessions/README.md — it does not
# run automatically. See that README for the full protocol.

slug="$1"
if [ -z "$slug" ]; then
  echo "usage: sh tools/session_new.sh <short-topic-slug>" >&2
  exit 2
fi

root="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
template="$root/docs/sessions/TEMPLATE.md"
if [ ! -f "$template" ]; then
  echo "missing template: $template" >&2
  exit 1
fi

day=$(date +%Y-%m-%d)
branch=$(git -C "$root" rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)
out="$root/docs/sessions/$day-$slug.md"
if [ -e "$out" ]; then
  echo "already exists: $out" >&2
  exit 1
fi

sed -e "s/^- Date:.*/- Date: $day/" \
    -e "s#^- Branch / worktree:.*#- Branch / worktree: $branch @ $root#" \
    -e "s/^- Status:.*/- Status: ACTIVE/" \
    "$template" > "$out"

echo "created ${out#"$root/"}"
echo "Fill in the goal and your file/module claim, then keep it current as you work."
