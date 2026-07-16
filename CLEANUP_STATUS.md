# Cleanup Inventory

Last reviewed: 2026-07-16

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
- Weather (`:data:weather`, all built-in providers, the weather widget and its `:data:widgets` type)
- Currency conversion (`:data:currencies`) and unit conversion (`:data:unitconverter`)
- The plugin system (`:plugins:sdk`, `:services:plugins`, `:data:plugins`, the plugin contracts in
  `:core:base` and `:core:shared`, the `de.mm20.launcher2:shared` Maven publication, and dokka)
- Device pose (`:core:devicepose`) — existed only to give the weather worker a location fix

## Still Present Technically

- Music service
- Calendar, widgets, tags, backup, and global actions
- Some settings routes, strings, serializers, and provider abstractions inherited from upstream
- Unreachable orphaned settings routes and screens (e.g., smartspacer, apps search settings)
- ~210 orphaned i18n strings in `core/i18n`, mostly `focus_*` keys with no current reader. Left in
  place deliberately: unlike the inherited-feature strings removed alongside their features, these
  are the product's own copy and may be staged for in-flight work.

Presence in this list does not mean a feature is reachable from the main UX. Check Gradle dependencies, application wiring, routes, settings entry points, and runtime references before describing a subsystem as removed.

## Focus Legacy State

- `FocusProfile` is not an active type in the current tree.
- `FocusTemporaryUnlock` remains intentionally as temporary per-app access state.
- Migration `35 -> 36` removes legacy custom attributes that used the old focus payload.
- Migration `37 -> 38` drops the removed features' tables (`forecasts`, `Currency`, `Plugins`) and
  any leftover weather widget row, and adds the focus-history indexes.
- Global classification remains in `focusEssentialAppKeys` and `focusDistractingAppKeys`.
- Focus settings live in a single `FocusSettingsScreen` hub; the legacy `FocusSystemSettingsScreen` and its `FocusSystemSettingsRoute`/`FocusSystemBasicsRoute` were removed. The `ROUTE_FOCUS_SYSTEM` deep link still resolves to the hub for back-compat.
- `focusDesaturateDistractingApps` is a live field despite its name: it backs the "fade distracting
  apps" toggle, which changes opacity, not saturation.

## Search Legacy State

- Product search returns installed applications plus pinned `shortcut`/`legacyshortcut` favorites.
  Nothing else exists: `SearchService` takes one application repository, `SearchResults` carries
  only apps, and `SearchFilters` is gone — there is nothing left to filter between.
- The core `File`, `Contact`, `Location`, `Website`, and `Article` searchable interfaces are deleted;
  with the plugin SDK gone, nothing could produce them.
- Historical preference fields for the removed search subsystems have been dropped. The DataStore
  serializer's `ignoreUnknownKeys = true` makes this safe for existing installs; DataStore
  migrations 3 and 4 now only carry the schema version forward.
- Search changes must audit ranking, hidden-item behavior, best-match launch, customization, and home launch surfaces together.

## Background Work

- The launcher runs no foreground service and no periodic background work. The only scheduled jobs
  are the one-shot, initial-delay `FocusSessionExpiryWorker` and `AppSessionExpiryWorker`.
- It requests no location at all; the `Location`, `Contacts`, `ExternalStorage` and `Call` groups
  are gone from `PermissionsManager`.
- The only network access is the user-configured Todoist integration. `INTERNET` is declared for it.

## Maintenance Rule

Update this inventory only when code evidence changes. Open or update a GitHub Issue for every actionable removal; do not add task checkboxes here.
