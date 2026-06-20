## 2024-06-20 - [Zip Bomb in Broadcast Receiver]
**Vulnerability:** GZIP bombs (a type of DoS attack) are possible if the app unconditionally parses compressed payloads from external sources. The `BreezyWeatherReceiver` received a GZIP byte array from intents and decoded it entirely into memory using `.readText()`.
**Learning:** External intents with `android:exported="true"` broadcast receivers can be triggered by arbitrary apps. Parsing unconstrained payloads like GZIP byte arrays could lead to `OutOfMemoryError` and application crashes, resulting in DoS.
**Prevention:** To prevent zip bombs and similar issues, always read input streams from untrusted external sources up to a maximum length limit using methods like `InputStream.readTextLimited(maxLength)`.
