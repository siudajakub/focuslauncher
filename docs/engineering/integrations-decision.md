# Integrations Decision

Last verified: 2026-07-16.

GitHub issue #4 is decided and closed. FocusLauncher keeps integrations that support a calm home or
focus planning and has physically removed the browsing, search, and platform subsystems it inherited
from Kvaesitso that did not fit the product.

The 2026-07-16 owner decision superseded the earlier "advanced / opt-in" tier: a feature is either
core to a focus-first launcher or it is deleted. Nothing is retained purely for upstream
compatibility.

## Retained

| Subsystem | Classification | Current role |
| --- | --- | --- |
| Calendar | Core | Focus scheduling, daily plan, upcoming events, calendar widget, and optional Tasks.org provider. |
| Widgets | Core | Launcher widget host, picker, and built-in calm-home widgets. |
| Favorites and tags | Core | Pinned items, visibility, organization, and launch history/ranking inputs. |
| Music | Core | Now-playing widget and clock part, driven by the notification listener. |
| Smartspacer and Tasks.org | Optional integrations | Reachable from their dedicated configuration surfaces when installed. |

## Physically Removed

The following are absent from `settings.gradle.kts`, application wiring, and the active source
graph:

- Weather (`:data:weather`, every built-in provider, the weather widget, and the weather widget
  type in `:data:widgets`).
- Currency conversion (`:data:currencies` and its hourly ECB exchange-rate worker).
- Unit conversion (`:data:unitconverter`).
- The plugin system (`:plugins:sdk`, `:services:plugins`, `:data:plugins`, the plugin contracts in
  `:core:base` and `:core:shared`, and the `de.mm20.launcher2:shared` Maven publication).
- Device pose (`:core:devicepose`) — it existed only to give the weather worker a location fix.
- Feed (`:services:feed` and its UI/settings surface).
- Contacts search (`:data:contacts`).
- Files/WebDAV search (`:data:files`, `:services:accounts`, `:libs:webdav`, Nextcloud, and Owncloud).
- Locations search (`:data:locations`).
- Calculator, website search, and Wikipedia search modules.

The generic `File`, `Contact`, `Location`, `Website`, and `Article` searchable contracts are also
gone: with the plugin SDK removed there was nothing left that could produce them.

## Search Consequence

The launcher query path returns installed applications, plus pinned `shortcut` / `legacyshortcut`
favorites matched by label — the set through which a PWA added to the home screen surfaces. There is
no other result type: `SearchService` takes a single application repository, and `SearchFilters` no
longer exists because there is nothing left to filter between.

Note that the pinned-shortcut set is not exclusively PWAs; any pinned launcher shortcut satisfies
it, and the launcher has no reliable PWA predicate.

This document records the durable product decision. Cleanup tasks and implementation status belong
in GitHub Issues and `PROJECT_STATUS.md` respectively.
