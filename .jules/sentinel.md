## 2024-06-19 - [Network Security Config]
**Vulnerability:** The application originally had `android:usesCleartextTraffic="true"`, permitting unencrypted HTTP communication that is susceptible to MITM attacks.
**Learning:** Explicit network security configuration must be configured since Android's default security postures may allow cleartext traffic for backwards compatibility or debugging depending on the target API.
**Prevention:** Disable cleartext traffic via `android:usesCleartextTraffic="false"` and define a strict `network_security_config.xml` to globally enforce HTTPS and restrict non-secure communication.
