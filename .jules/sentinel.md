## 2026-06-26 - [MITM Prevention Enhancement]
**Vulnerability:** The application previously allowed cleartext traffic (`android:usesCleartextTraffic="true"`) in the manifest, increasing the risk of Man-in-the-Middle (MITM) attacks where data can be intercepted or manipulated in transit.
**Learning:** The explicit allowance of cleartext traffic in a secure application undermines data integrity and confidentiality, especially when communicating with APIs or web services, which should default to HTTPS.
**Prevention:** Ensured `android:usesCleartextTraffic="false"` is set in `app/app/src/main/AndroidManifest.xml` to enforce strict HTTPS communication for all application components and prevent future misconfigurations.
