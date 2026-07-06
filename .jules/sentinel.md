## 2025-02-23 - Fix Path Traversal (Zip Slip) in Backup Manager
**Vulnerability:** Found a classic Zip Slip vulnerability in `BackupManager.kt` where unzipping an archive using `ZipInputStream` did not validate if the entry names (which could contain `../`) would extract outside the intended restore directory.
**Learning:** The custom backup system manually extracted entries directly using `File(outDir, entry.name)` without verifying the resolved canonical path, allowing potential arbitrary file overwrite.
**Prevention:** Always validate that `file.canonicalPath.startsWith(canonicalOutDir + File.separator)` before extracting archive entries.
