## 2025-02-14 - Fix Zip Slip (Path Traversal) Vulnerability
**Vulnerability:** Path traversal (Zip Slip) vulnerability during backup restoration. Extracting a zip entry with a name like `../../evil.txt` could result in files being written outside the intended destination directory.
**Learning:** `java.util.zip.ZipInputStream` does not automatically validate or sanitize zip entry names, making it susceptible to path traversal.
**Prevention:** Always validate extracted file paths. Check that the canonical path of the extracted file starts with the target directory's canonical path plus `File.separator`.
