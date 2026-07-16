---
sidebar_position: 3
---

# Modules

`settings.gradle.kts` is the source of truth for the active Gradle graph. The current project is
split into these groups:

## Application

- `:app:app`: application ID, variants, signing, manifest merge, Koin bootstrap, and
  `LauncherApplication`.
- `:app:ui`: launcher/settings activities, Compose UI, search orchestration, focus gate, and
  platform UsageStats access.

## Services

- `:services:focus`: focus classification, launch policy/coordinator, session lifecycle, history,
  reports, usage-summary models, and expiry worker.
- `:services:search`: application/shortcut/tool search orchestration; the active launcher UI
  currently requests apps only.
- `:services:favorites`, `:services:tags`: pinned items, ranking/visibility history, and tags.
- `:services:icons`, `:services:badges`: icon resolution/transforms and badges.
- `:services:widgets`: high-level widget APIs.
- `:services:backup`: backup and restore.
- `:services:music`: media sessions and now-playing metadata.
- `:services:global-actions`: system actions such as notifications and screen lock.

## Data

- `:data:applications`, `:data:appshortcuts`, `:data:searchable`: installed apps, shortcuts, and
  persisted searchable items.
- `:data:database`: Room database, DAOs, migrations, and exported schema.
- `:data:customattrs`: per-item labels, icons, tags, and temporary focus access.
- `:data:calendar`: Android and Tasks.org calendar providers.
- `:data:widgets`, `:data:notifications`: widget storage and notification access.
- `:data:themes`, `:data:i18n`: theme and localization data.

## Core

- `:core:base`: shared searchable contracts, models, and utilities.
- `:core:preferences`: DataStore model and typed settings wrappers.
- `:core:i18n`: canonical translatable resources.
- `:core:permissions`, `:core:profiles`, `:core:compat`: permissions, Android profiles, and
  compatibility helpers.
- `:core:crashreporter`, `:core:ktx`, `:core:shared`: diagnostics, extensions, and shared contracts.

## Libraries

- `:libs:address-formatter`, `:libs:material-color-utilities`: vendored standalone libraries.

Plugins, weather, currency/unit conversion, contacts, files, places/locations, website, and
Wikipedia search modules have been removed. See the repository
[architecture guide](../../../engineering/architecture.md) and
[integration decision](../../../engineering/integrations-decision.md).
