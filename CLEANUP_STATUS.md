# Cleanup Status

Ten plik jest roboczą listą zmian porządkowych w launcherze.
Zbieramy tu rzeczy, które:

- zostały już usunięte z głównego UX, ale kod nadal istnieje,
- są częściowo przepięte na nowy model produktu,
- wymagają późniejszego doczyszczenia, żeby repo nie obrastało martwymi ścieżkami.

## Zasada

Jeśli coś:

- znika z ustawień lub głównego flow,
- zostaje przeniesione do `Advanced`,
- albo przestaje być źródłem prawdy, ale nadal istnieje technicznie,

to dopisujemy to tutaj.

## Aktualny Stan

### Focus

- `FocusProfile` nadal istnieje technicznie w:
  - `data/customattrs/.../FocusProfile.kt`
  - `CustomAttributesRepository`
  - części runtime focus
- `Morning mode` i `Sleep mode` zostały zastąpione produktowo przez `Productivity time`.
- `Focus Home` został usztywniony jako stały główny home produktu.
- `Commute Quick Actions` i `Focus at a Glance` zostały usunięte z aktywnego produktu.
- `Emergency Bypass` został wycofany z aktywnego produktu i nie steruje już głównym UX ani polityką focus.
- Nowy model czasu produktywnego to:
  - `focusProductivityTimeEnabled`
  - `focusProductivityWindow1StartMinutes`
  - `focusProductivityWindow1EndMinutes`
  - `focusProductivityWindow2StartMinutes`
  - `focusProductivityWindow2EndMinutes`
- Produktowo model per-app nie jest już głównym modelem UX.
- Docelowy model produktu to:
  - `focusEssentialAppKeys`
  - `focusDistractingAppKeys`
  - globalne reguły dla `Distracting`
- Do dalszego cleanupu:
  - usunąć stare pola `focusMorning*` i `focusSleep*` z modelu danych po bezpiecznym okresie przejściowym,
  - odciąć runtime od `FocusProfile` wszędzie tam, gdzie służy już tylko jako stary nośnik konfiguracji,
  - ograniczyć `FocusProfile` do minimum albo całkiem go usunąć po migracji architektury.

### Search

- `Search settings` zostały mocno odchudzone produktowo.
- Search działa już produktowo jako `apps-only`.
- Z aktywnego UX zniknęły:
  - tagi w search/customize flow,
  - filtr categories / filter bar,
  - przełącznik `strict search` jako osobna decyzja użytkownika.
- Z głównego UX odłączono lub ograniczono rolę dawnych subsystemów search, takich jak:
  - Wikipedia
  - file search settings
  - calendar search settings
  - contacts search settings
  - locations settings
  - search actions settings
- Część ekranów i route'ów nadal istnieje w kodzie.
- Do dalszego cleanupu:
  - usunąć nieużywane route'y z kodu, jeśli nie są już potrzebne nawet technicznie,
  - sprawdzić, czy helpery i ViewModel-e powiązane z tymi ekranami nadal są do czegokolwiek używane.

### Settings Structure

- Główne settingsy zostały uproszczone do:
  - Appearance
  - Home screen
  - Focus mode
  - Language and region
  - Advanced
- `Advanced` przejęło rzeczy drugorzędne i techniczne.
- Do dalszego cleanupu:
  - zdecydować, czy `Icons` i `Gestures` mają zostać w `Advanced`, czy też zejść jeszcze głębiej albo zostać uproszczone bardziej radykalnie,
  - sprawdzić, czy jakieś stare entrypointy nadal otwierają odłączone ekrany bocznymi ścieżkami.

### Gestures

- `Double tap -> screen off` zostało usunięte z aktywnego produktu.
- W danych kompatybilnościowych nadal może istnieć stare pole `gesturesDoubleTap`, ale nie jest już częścią aktywnego UX launchera.
- Do dalszego cleanupu:
  - rozważyć fizyczne usunięcie starego pola po bezpiecznej migracji danych.

### Integrations / Technical Screens

- `Integrations` zostały ograniczone do bardziej użytkowych rzeczy.
- Z głównego produktu spadły na margines:
  - plugins
  - backup
  - debug
  - about
- Część technicznych ekranów nadal istnieje i jest osiągalna przez `Advanced`.
- Do dalszego cleanupu:
  - zdecydować, które rzeczy mają zostać tylko dla developera,
  - usunąć z produktu wszystko, co nie wspiera focus-first nawet jako opcja.

### Home / Widgets

- `Focus home` jest głównym modelem produktu.
- Widgety zostały zachowane jako opcja.
- `HomescreenSettingsScreen` został uporządkowany, ale nadal zawiera dużo starej elastyczności launchera.
- Do dalszego cleanupu:
  - ograniczyć rozbudowane widget-heavy ścieżki,
  - uprościć homescreen settings do modelu „focus first, widgets optional”.

## Rzeczy Do Sprawdzenia Przy Każdym Kolejnym Etapie

- Czy dana funkcja została tylko schowana z UX, czy naprawdę przestała być używana?
- Czy istnieją route'y, importy, ViewModel-e lub stringi dla rzeczy już nieużywanych?
- Czy nowy model produktu ma tylko jedno źródło prawdy?
- Czy zostawiony kod techniczny nadal ma uzasadnienie?

## Następne Kandydaty Do Fizycznego Usunięcia Z Kodu

- stare search settings screens odłączone z głównego UX,
- resztki per-app focus runtime po pełnym odcięciu od `FocusProfile`,
- entrypointy do funkcji historycznych, które nie mają już roli produktowej,
- stringi i copy po starym modelu launchera.
