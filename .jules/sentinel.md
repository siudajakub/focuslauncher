## 2024-06-28 - [Prevent Man-in-the-Middle (MITM) attacks by disabling cleartext traffic]
**Vulnerability:** The application was allowing cleartext (HTTP) traffic because `android:usesCleartextTraffic` was set to "true" in `app/app/src/main/AndroidManifest.xml`. This could allow attackers to intercept and read or modify sensitive data over the network (MITM).
**Learning:** Enforcing HTTPS at the manifest level by explicitly setting `android:usesCleartextTraffic="false"` is a critical defense-in-depth measure. Applications often default to true or it's left on for debugging, which leaks into production.
**Prevention:** Always verify that `android:usesCleartextTraffic` is set to `false` in the production manifest to ensure the app relies exclusively on secure TLS connections.
