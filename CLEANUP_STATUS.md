# Cleanup Inventory

Last reviewed: 2026-06-28

This is a factual inventory of legacy surface still present in the current tree. Actionable cleanup work belongs in GitHub Issues under the `cleanup` label.

## Removed From The Active Gradle Graph

- Calculator search
- Website search
- Wikipedia search
- Nextcloud integration
- Owncloud integration
- Feed (`:services:feed`, FeedComponent, FeedSettings) — removed per the #4 decision
- Contacts search (`:data:contacts`, ContactSearchSettings) — removed per the #4 decision
- Files search (`:data:files`, FileSearchSettings) — removed per the #4 decision
- Locations search (`:data:locations`, LocationSearchSettings) — removed per the #4 decision
- WebDAV backend (`:libs:webdav`) and accounts (`:services:accounts`, Nextcloud/Owncloud `AccountType`) — removed with Files search

## Still Present Technically

- Weather data and widgets (advanced-only / opt-in)
- Plugin SDK, plugin data, and plugin services (developer-only)
- Music service
- Unit converter data and UI dependencies (advanced-only)
- Calendar, widgets, tags, backup, and global actions
- Core `File`/`Contact`/`Location` searchable interfaces and their plugin SDK contracts — kept after the search subsystems were removed (no producers remain; inert).
- Unused i18n strings for the removed search subsystems across `core/i18n` locale files (harmless; out of scope for build).
- Orphaned persisted fields for removed search providers (`fileSearchProviders`, `contactSearch*`, `locationSearch*`, `feedProviderPackage`) — kept; some are read by historical DataStore migrations and the serializer ignores unknown keys.
- Some settings routes, strings, serializers, and provider abstractions inherited from upstream
- Unreachable orphaned settings routes and screens (e.g., smartspacer, apps search settings).
- Stale routes in SettingsActivity (ROUTE_WEATHER_INTEGRATION, ROUTE_MEDIA_INTEGRATION) that will crash if invoked by widgets.
- Serialization-coupled enum/sealed cases left for compatibility: `KeyboardFilterBarItem.Contacts`, `GestureAction.Feed` (no longer writable; dispatch is a no-op).
- Hardcoded Wikipedia/Websites strings in KeyboardFilterBarItem.

Presence in this list does not mean a feature is reachable from the main UX. Check Gradle dependencies, application wiring, routes, settings entry points, and runtime references before describing a subsystem as removed.

## Focus Legacy State

- `FocusProfile` is not an active type in the current tree.
- `FocusTemporaryUnlock` remains intentionally as temporary per-app access state.
- Migration `35 -> 36` removes legacy custom attributes that used the old focus payload.
- Global classification remains in `focusEssentialAppKeys` and `focusDistractingAppKeys`.

## Search Legacy State

- Product search is apps-first.
- Historical preference fields for calculator, websites, and Wikipedia have been removed; the DataStore serializer's `ignoreUnknownKeys = true` makes dropping unused keys safe for existing installs. File-provider (WebDAV) fields remain pending the integrations decision (#4).
- Search changes must audit ranking, filters, hidden-item behavior, best-match launch, customization, and home launch surfaces together.

## Maintenance Rule

Update this inventory only when code evidence changes. Open or update a GitHub Issue for every actionable removal; do not add task checkboxes here.
