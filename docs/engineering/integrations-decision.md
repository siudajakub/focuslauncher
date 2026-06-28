# Integrations Decision

Last reviewed: 2026-06-28

Proposed product note. Every classification below is **Proposed — needs owner sign-off**.
This is a product call that belongs to the human owner; nothing here is final and no code
has been changed.

## Criteria

Each optional subsystem is judged against one question: does it serve a calm, focus-first
launcher that helps people use their phone deliberately? Supporting signals:

- **Focus relevance:** does it advance classification, sessions, policy, or essential-app access?
- **Live wiring:** is it reachable from the active UX (search, home, widgets, settings, focus), or only registered in Koin and otherwise orphaned?
- **Blast radius:** how much module surface and persisted state removal would touch.

Classifications: `core` (keep, integral to the product), `advanced-only` (keep but gate behind
opt-in/advanced surface), `developer-only` (keep for plugin authors or debugging, not end users),
`removal-candidate` (propose physical removal once the owner approves).

## Classification

| Subsystem | Where it lives (module / path) | Proposed classification | Rationale |
| --- | --- | --- | --- |
| Calendar | `data/calendar`; consumed by `app/ui/.../focus/plan/FocusPlanVM.kt`, `.../settings/focussystem`, `.../focusschedule`, `.../widgets/calendar` | `core` | Drives the focus daily schedule and focus plan, not just a widget — integral to focus features. |
| Widgets | `data/widgets`, `services/widgets`; host in `app/ui/.../base/AppWidgetHost.kt`, picker in `.../sheets/WidgetPickerSheet*.kt` | `core` | Live widget host and picker back the home surface; built-in widgets (apps, notes, calendar) are the calm home. |
| Unit conversion | `data/unitconverter`, `data/currencies`; wired into `services/search/.../SearchService.kt` via `filters.tools` | `advanced-only` | The only non-app result type actually searched; useful but peripheral, so gate behind the tools filter rather than promote. |
| Weather | `data/weather`; widget in `app/ui/.../widgets/weather`, dialog in `.../common/WeatherLocationSearchDialog*.kt` | `advanced-only` | Reachable only as an opt-in built-in widget; ambient, not focus-essential. Keep behind widget opt-in. |
| Music | `services/music`; widget in `app/ui/.../widgets/music`, clock part in `.../clock/parts/MusicPartProvider.kt` | `advanced-only` | Now-playing widget/clock part only; convenient but a distraction surface, so keep opt-in, not default. |
| Plugins | `plugins/sdk`, `data/plugins`, `services/plugins`; Koin-registered in `app/app/.../LauncherApplication.kt` | `developer-only` | Public plugin contract retained from upstream with no end-user settings screen; keep for plugin authors, not surfaced to users. |
| Feed | `services/feed`; `app/ui/.../scaffold/FeedComponent.kt` (imported but mounted in no active scaffold page) | `removal-candidate` | Orphaned — the component is never placed in a live scaffold configuration; a content feed contradicts a low-browsing product. |
| Contacts | `data/contacts`; only Koin-registered in `LauncherApplication.kt`, no UI/search/focus consumer | `removal-candidate` | Not searched and not rendered anywhere; pure dead module weight. |
| Files | `data/files`; only Koin-registered in `LauncherApplication.kt` (plus `services/badges` build dep), no UI/search consumer | `removal-candidate` | Not searched and not rendered; file browsing is off-product for a focus launcher. |
| Locations | `data/locations`; only Koin-registered in `LauncherApplication.kt`, no UI/search/focus consumer | `removal-candidate` | Not searched and not rendered; nearby-places search is off-product. |

Active search (`services/search/.../SearchService.kt`) covers only apps, app shortcuts, and unit
converters. Calendar, contacts, files, locations, and weather have no live search path — their
classifications above reflect non-search consumers (or the absence of any consumer).

## Removal candidates → follow-up issues

File these only if the owner approves removal. Each must audit the Gradle graph, Koin wiring in
`LauncherApplication.kt`, routes, strings, serializers, migrations, and tests before deletion,
per `docs/engineering/architecture.md`.

- **Feed:** remove `services/feed` and the orphaned `app/ui/.../scaffold/FeedComponent.kt`; drop the `:services:feed` deps in `app/ui` and `app/app` and the `feedModule` registration.
- **Contacts:** remove `data/contacts`; drop the `:data:contacts` deps in `app/ui` and `app/app` and the contacts Koin module from `LauncherApplication.kt`.
- **Files:** remove `data/files`; drop the `:data:files` deps in `app/ui`, `app/app`, and `services/badges`, and the files Koin module; confirm `services/badges` needs no file-badge path.
- **Locations:** remove `data/locations` (and assess `libs/address-formatter`, `core/devicepose` if they exist only for locations); drop the `:data:locations` deps and Koin module.

## Decisions needed from owner

- Confirm **calendar** stays `core` despite being an upstream integration, on the grounds that focus scheduling depends on it.
- Confirm **weather** and **music** stay as `advanced-only` opt-in widgets rather than being removed as distraction surfaces.
- Decide whether the **plugin SDK** is kept as a `developer-only` extension point at all, or removed to shrink the surface; this gates the contacts/files/locations removals if any are intended to return as plugins.
- Approve (or reject) each **removal-candidate** — feed, contacts, files, locations — before any cleanup issue is filed.
- Decide whether **unit conversion** stays behind the tools filter or is removed as the last non-app search type.
