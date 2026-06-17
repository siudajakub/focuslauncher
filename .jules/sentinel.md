## 2024-05-24 - Critical Manifest Security Hardening
**Vulnerability:** The main `AndroidManifest.xml` had `android:allowBackup="true"` and `android:usesCleartextTraffic="true"`, allowing physical attackers to extract sensitive app data via ADB backup and exposing network communications to Man-in-the-Middle (MITM) attacks.
**Learning:** Even though the app uses its own backup system, default Android backup mechanisms can expose local files. The `usesCleartextTraffic="true"` setting undermines all HTTPS expectations.
**Prevention:** Always set `android:allowBackup="false"` unless explicitly needed and audited. Globally disable cleartext traffic via `usesCleartextTraffic="false"` and enforce it in a separate `network_security_config.xml` to prevent data leakage and MITM risks.
