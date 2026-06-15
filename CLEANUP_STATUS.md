# Cleanup Inventory

Last reviewed: 2026-06-15

This is a factual inventory of legacy surface still present in the current tree. Actionable cleanup work belongs in GitHub Issues under the `cleanup` label.

## Removed From The Active Gradle Graph

- Calculator search
- Website search
- Wikipedia search
- Nextcloud integration
- Owncloud integration

## Still Present Technically

- Weather data and widgets
- Plugin SDK, plugin data, and plugin services
- Feed and music services
- Unit converter data and UI dependencies
- Calendar, contacts, files, locations, widgets, tags, backup, and global actions
- Compatibility preference fields for removed search providers and historical behavior
- Some settings routes, strings, serializers, and provider abstractions inherited from upstream
- Unreachable orphaned settings routes and screens (e.g., smartspacer, apps search settings).
- Stale routes in SettingsActivity (ROUTE_WEATHER_INTEGRATION, ROUTE_MEDIA_INTEGRATION) that will crash if invoked by widgets.
- Legacy preferences in LauncherSettingsData.kt and FileSearchSettings.kt for Calculator, Wikipedia, Nextcloud, Owncloud.
- Vestigial AccountType enum containing only removed Nextcloud/Owncloud.
- Hardcoded Wikipedia/Websites strings in KeyboardFilterBarItem.

Presence in this list does not mean a feature is reachable from the main UX. Check Gradle dependencies, application wiring, routes, settings entry points, and runtime references before describing a subsystem as removed.

## Focus Legacy State

- `FocusProfile` is not an active type in the current tree.
- `FocusTemporaryUnlock` remains intentionally as temporary per-app access state.
- Migration `35 -> 36` removes legacy custom attributes that used the old focus payload.
- Global classification remains in `focusEssentialAppKeys` and `focusDistractingAppKeys`.

## Search Legacy State

- Product search is apps-first.
- Historical preference fields for calculator, websites, Wikipedia, and file providers remain for compatibility and require deliberate migration before deletion.
- Search changes must audit ranking, filters, hidden-item behavior, best-match launch, customization, and home launch surfaces together.

## Maintenance Rule

Update this inventory only when code evidence changes. Open or update a GitHub Issue for every actionable removal; do not add task checkboxes here.
