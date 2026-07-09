## 2024-07-09 - Missing ARIA Labels on Icon-only Buttons
**Learning:** Found that multiple icon buttons in FavoritesTagSelector.kt lacked accessibility labels (contentDescription = null), making them unusable for screen readers. In Compose, Icon buttons must have valid contentDescriptions using string resources.
**Action:** Always add a stringResource to contentDescription for IconButtons instead of null.
