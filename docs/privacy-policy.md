# Privacy Policy

This policy explains what data FocusLauncher processes, how it is used, and the measures taken to
protect it. FocusLauncher is a focus-first Android launcher. It runs no servers of its own and
performs no analytics or tracking: by default all data described below stays on your device, in an
app-private directory that other apps and users cannot access, and nothing is sent to us.

The exception is optional features you explicitly enable that contact a third party — most notably
the Weather feature, which sends your location to a third-party weather provider to fetch a
forecast. These are described in "Data Protection and Network Access" below.

## 1. Data FocusLauncher Processes

To provide search and focus features, FocusLauncher reads and stores the following on your device:

- **Installed apps:** Names and identifiers of apps installed on your device, used to list and
  search for apps.
- **App usage frequency:** How often you launch apps, used to rank search results and inform focus
  features. This is derived locally; FocusLauncher does not upload usage data.
- **Focus state, sessions, and history:** Your app classifications (which apps are treated as
  essential or distracting), active and past focus sessions, temporary unlocks, daily-limit
  counters, and the focus history used to show your weekly focus insights.
- **Time-awareness usage access:** When you enable Time Awareness, FocusLauncher uses the system
  usage-access permission to read foreground-app and screen-time information so it can surface
  periodic time-awareness reminders. This information is processed on-device only.
- **Quick Capture notes:** Notes you write with Quick Capture are stored locally. They are shared
  only when you explicitly choose to share a note.
- **Calendar events (optional):** If you grant calendar access, event details (such as title, time,
  location, and description) are read locally to show them in search.
- **Location (only for Weather):** If you enable the optional Weather feature with automatic
  location, FocusLauncher uses your device location to request a forecast. It is used only for that
  purpose; see "Data Protection and Network Access".
- **Crash and diagnostic reports:** Technical details about crashes and errors, used for debugging.
  These are stored locally and are never sent automatically.

## 2. How Your Data Is Used

- **Local processing only:** All of the above is processed on your device to power search, app
  ranking, focus sessions, daily limits, time awareness, and quick capture. There is no backend
  service, account, or cloud sync.
- **Search results:** Search results are generated locally from on-device data. FocusLauncher does
  not send your search queries to any external search service.
- **Crash reporting:** Crash and diagnostic reports are stored locally and shared only if you choose
  to do so (for example, by attaching them to a GitHub issue).

## 3. Data Protection and Network Access

- **Local storage:** All on-device data is stored in a secure, app-specific directory that other
  apps cannot read.
- **No backend or analytics of ours:** FocusLauncher has no server and collects no analytics or
  telemetry. We receive nothing.
- **Network and third-party services:** Some optional features reach the network only when you turn
  them on:
    - **Weather (optional):** When enabled, FocusLauncher sends your location (approximate or
      precise, per your choice) to a third-party weather provider to fetch a forecast. That request
      is handled by the provider under its own privacy policy. Disabling Weather stops it.
    - **Any integration you connect** (for example a tasks/to-do provider) communicates with that
      service using credentials you supply, and only while it is connected.

  Apart from these, the only network activity is the normal launching of other apps and any web
  links you choose to open in your browser.
- **Your control:** You can remove all stored data at any time by clearing the app's storage or
  uninstalling FocusLauncher, and you can disable any optional networked feature in settings.

## 4. Permissions

FocusLauncher requests only the permissions needed for its features, and each is optional unless
required for core launcher behavior:

- **Usage access:** Required for app-usage ranking and the Time Awareness feature.
- **Notifications:** Used to deliver time-awareness reminders and focus-related notifications.
- **Location:** Optional; used only by the Weather feature, and only if you enable it with automatic
  location.
- **Calendar:** Optional; only used if you want calendar events in focus scheduling and search.
- **Query installed apps:** Used to list and search the apps on your device — a launcher's core
  function.

Sharing notes or logs and backing up your data use the Android system file picker and share sheet,
so they need no storage permission.

You can grant or revoke these permissions at any time in your device settings; revoking a permission
disables the feature that depends on it.

## 5. Crash Reports

Crash reports are stored locally and never shared automatically. They may include:

- Technical details about the crash.
- Device information (for example, model and operating system version).

You can share crash reports manually (for example, via GitHub). Note that anything you share
manually may become public and is then subject to the receiving platform's privacy policy (such as
the GitHub Privacy Policy).

## 6. Your Rights and Control

- **Data access:** All data resides on your device, so you retain full control over it.
- **Data deletion:** Clearing the app's data or uninstalling FocusLauncher removes all stored
  information.
- **Opt-out:** Optional features and their permissions can be disabled at any time.
