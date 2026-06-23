## 2024-06-23 - Disable Cleartext Traffic to Prevent MITM Attacks
**Vulnerability:** The application was allowing cleartext (HTTP) traffic because `android:usesCleartextTraffic` was explicitly set to `true` in `app/app/src/main/AndroidManifest.xml`. This makes the application vulnerable to Man-in-the-Middle (MITM) attacks where an attacker can intercept and modify network traffic.
**Learning:** By explicitly allowing cleartext traffic, any insecure HTTP calls made by the application, whether by design or accident, are permitted by the OS, exposing user data.
**Prevention:** Enforce HTTPS across the application by setting `android:usesCleartextTraffic="false"` in the application manifest. Always use secure protocols (HTTPS) for any network communication.
