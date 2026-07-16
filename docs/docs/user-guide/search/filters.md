# Search Scope

FocusLauncher currently uses a fixed, local, apps-first query:

- installed applications are searched and focus-ranked;
- pinned shortcut favorites are matched separately;
- hidden apps can appear through the dedicated hidden-items flow;
- calendar events are not returned by search (they surface in the calendar widget and Focus Plan
  instead).

There is no user-facing filter menu or settings route to change what search covers.

::: warning
Pinned shortcuts currently launch outside the owning application's focus policy. Do not rely on
them for session blocking or daily limits until
[issue #98](https://github.com/siudajakub/focuslauncher/issues/98) is fixed.
:::
