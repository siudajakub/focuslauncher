
## 2024-05-24 - [Fix Path Traversal (Zip Slip) in Backup Extraction]
**Vulnerability:** A Zip Slip vulnerability (Path Traversal) existed in `BackupManager.extractArchive` when unpacking backup files. Maliciously crafted zip archives could overwrite files outside the intended destination directory if the `ZipEntry` name contains directory traversal sequences (e.g., `../`).
**Learning:** `java.util.zip.ZipInputStream` does not automatically protect against directory traversal in zip entry names. Extracted files must always be validated against the target canonical directory path.
**Prevention:** Always check if `file.canonicalPath.startsWith(canonicalOutDir)` when extracting archive entries to ensure they stay within the intended extraction directory.
