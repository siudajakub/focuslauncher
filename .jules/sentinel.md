## 2024-07-10 - Zip Slip Vulnerability in BackupManager
**Vulnerability:** Path traversal (Zip Slip) vulnerability found in `BackupManager.kt`'s `extractArchive` method where entries in a zip file could extract files outside the designated output directory.
**Learning:** It existed because `ZipInputStream` entries do not enforce their names to be safe, allowing entries like `../foo` to be written directly if unvalidated.
**Prevention:** Always validate that the canonical path of the extracted file starts with the target directory's canonical path plus a file separator before extracting from an archive.
