## 2024-05-15 - [Cleartext Traffic Disabled]
**Vulnerability:** Android application manifest explicitly enabled cleartext traffic (`android:usesCleartextTraffic="true"`), which could allow Man-in-the-Middle (MITM) attacks and data interception.
**Learning:** The application was not enforcing HTTPS, potentially exposing sensitive user data when communicating over the network.
**Prevention:** Explicitly disable cleartext traffic (`android:usesCleartextTraffic="false"`) or use a Network Security Configuration file to strictly enforce HTTPS for all network requests.
