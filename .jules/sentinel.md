## 2024-07-13 - [Zip Slip Vulnerability in Backup extraction]
**Vulnerability:** Path traversal vulnerability (Zip Slip) found in `BackupManager.extractArchive` when unpacking backup files. Unsanitized entry names from the zip could allow files to be extracted outside the destination directory.
**Learning:** When extracting ZIP files, checking only `File(outDir, entry.name)` is not enough. The `java.util.zip.ZipInputStream` does not validate paths natively.
**Prevention:** Always validate the canonical path of the extracted file against the destination directory's canonical path plus a file separator before extraction.
