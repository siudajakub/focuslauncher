## 2024-05-24 - Missing content descriptions on icon-only buttons
**Learning:** Found a common pattern where `IconButton` composables wrapping an `Icon` component have `contentDescription = null`. This creates a severe accessibility barrier because screen readers have no text to announce, leaving users unaware of the button's purpose (e.g., share, bug report).
**Action:** When adding icon-only buttons, always ensure a localized `contentDescription` is provided using `stringResource()` so that screen readers can accurately describe the action.
