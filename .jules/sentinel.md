## 2025-02-14 - Prevent Cleartext Traffic
**Vulnerability:** Android application allows unencrypted (cleartext) HTTP traffic via `android:usesCleartextTraffic="true"` in the AndroidManifest.xml. This exposes the app to Man-in-the-Middle (MITM) attacks where attackers could intercept and read or modify data in transit.
**Learning:** Permitting cleartext traffic should be explicitly disabled, particularly for an app that handles user data, settings, or external integrations.
**Prevention:** Always enforce HTTPS by explicitly setting `android:usesCleartextTraffic="false"` in the primary `AndroidManifest.xml`.
