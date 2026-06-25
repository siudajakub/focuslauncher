## 2024-06-25 - [Manifest Hardening: Prevent Cleartext Traffic]
**Vulnerability:** The Android Manifest file `app/app/src/main/AndroidManifest.xml` explicitly permitted cleartext network traffic (`android:usesCleartextTraffic="true"`).
**Learning:** This exposes network requests to interception, potentially allowing Man-in-the-Middle (MITM) attacks if non-HTTPS connections are made, a significant risk for the entire application.
**Prevention:** Always enforce HTTPS-only connections by setting `android:usesCleartextTraffic="false"` (which is the default on Android 9+, but explicit definition is safer) in the Manifest unless there's an extremely specific and documented reason for exceptions.
