## 2024-06-30 - Fix DoS vulnerability in BreezyWeatherReceiver
**Vulnerability:** The application was vulnerable to Denial of Service (Zip/GZip Bomb) attacks due to reading an untrusted `GZIPInputStream` directly into memory using `readText()` without any size limits in `BreezyWeatherReceiver.kt`. This could lead to `OutOfMemoryError` and application crashes.
**Learning:** External data streams, especially compressed ones, must not be directly decompressed into memory without setting upper bound limits. `readText()` on an unconstrained stream is unsafe for untrusted input.
**Prevention:** Always use safe stream reading methods that enforce a maximum size constraint, such as the newly created `readTextLimited(maxLength: Int)` extension function for `InputStream`.
