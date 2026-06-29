## 2026-06-29 - Added missing a11y contentDescriptions to icon-only buttons
**Learning:** Icon-only buttons used in settings screens for crash reports and logs frequently lack proper `contentDescription` string resource mappings, which is a key accessibility omission.
**Action:** When working on UI components in `app/ui/.../settings`, ensure that screen readers can interpret icon buttons, specifically for common actions like sharing or reporting bugs.
