# Privacy Policy

This policy explains what data FocusLauncher processes, how it is used, and the measures taken to
protect it. FocusLauncher is a focus-first Android launcher. It runs no servers of its own and
performs no analytics or tracking: by default all data described below stays on your device, in an
app-private directory that other apps and users cannot access, and nothing is sent to us.

FocusLauncher makes no automatic or background network requests. The only outbound network traffic
is the optional Todoist focus-plan integration, and only after you configure it yourself with your
own API token; see "Data Protection and Network Access" below.

## 1. Data FocusLauncher Processes

To provide search and focus features, FocusLauncher reads and stores the following on your device:

- **Installed apps:** Names and identifiers of apps installed on your device, used to list and
  search for apps.
- **App usage frequency:** How often you launch apps, used to rank search results and inform focus
  features. This is derived locally; FocusLauncher does not upload usage data.
- **Focus state, sessions, and history:** Your app classifications (which apps are treated as
  essential or distracting), active and past focus sessions, temporary unlocks, daily-limit
  counters, and the focus history used to show your weekly focus insights.
- **Usage access:** If you grant Usage Access, FocusLauncher reads foreground-app events and per-app
  foreground time locally to power the optional "today on distracting apps" summary on the Focus
  Home. This data is not uploaded.
- **Quick Capture notes:** Notes you write with Quick Capture are stored locally. They are shared
  only when you explicitly choose to share a note.
- **Calendar events (optional):** If you grant calendar access, event details (such as title, time,
  location, and description) are read locally for focus scheduling, the daily plan, upcoming-event
  cards, and calendar widgets. The active apps-first search does not return calendar results.
- **Crash and diagnostic reports:** Technical details about crashes and errors, used for debugging.
  These are stored locally and are never sent automatically.

## 2. How Your Data Is Used

- **Local processing only:** All of the above is processed on your device to power search, app
  ranking, focus sessions, daily limits, and quick capture. There is no backend service, account, or
  cloud sync.
- **Search results:** Search results are generated locally from on-device data. FocusLauncher does
  not send your search queries to any external search service.
- **Crash reporting:** Crash and diagnostic reports are stored locally and shared only if you choose
  to do so (for example, by attaching them to a GitHub issue).

## 3. Data Protection and Network Access

- **Local storage:** All on-device data is stored in a secure, app-specific directory that other
  apps cannot read.
- **No backend or analytics of ours:** FocusLauncher has no server and collects no analytics or
  telemetry. We receive nothing.
- **Network and third-party services:** FocusLauncher makes no automatic or background network
  requests. It reaches an external service only when you explicitly connect one:
    - **The built-in Todoist focus-plan integration** sends your stored API token to Todoist and
      fetches active tasks, but only once you configure that integration with your own token and the
      plan is loaded. No other integration is currently built in.

  Apart from that, network activity comes from normal launching of other apps and web links you
  choose to open in your browser.
- **Your control:** You can remove all stored data at any time by clearing the app's storage or
  uninstalling FocusLauncher. Connected integrations such as Todoist can be disconnected at any time
  in their settings.

## 4. Permissions

FocusLauncher requests only the permissions needed for its features, and each is optional unless
required for core launcher behavior:

- **Usage access:** Optional; used for distracting-app foreground-time reflection. Search ranking
  uses the launcher's own local launch history, not Usage Access.
- **Notifications:** Used to deliver focus-related notifications.
- **Calendar:** Optional; used for focus scheduling, the daily plan, calendar widgets, and creating
  scheduled items. Calendar events are not returned by the active apps-first search.
- **Query installed apps:** Used to list and search the apps on your device — a launcher's core
  function.
- **Notification policy access:** Optional; lets a focus session enable Do Not Disturb and restore
  the previous interruption filter when the session ends.

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
