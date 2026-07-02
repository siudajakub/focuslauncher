## 2024-05-24 - Zip Slip in BackupManager
**Vulnerability:** Path Traversal (Zip Slip) in BackupManager.kt during backup extraction using ZipInputStream.
**Learning:** `ZipInputStream` does not automatically validate paths. Using `entry.name` directly in `File(outDir, entry.name)` allows an attacker to use `../` to write outside the intended directory.
**Prevention:** Always validate the canonical path of extracted entries against the target directory's canonical path before writing files.
