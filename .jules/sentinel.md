## 2024-05-15 - [Path Traversal in Zip Extraction]
**Vulnerability:** The `extractArchive` function in `BackupManager.kt` extracted entries from a zip file without validating if the resulting file path was inside the intended output directory, allowing for a Zip Slip vulnerability where an attacker could overwrite arbitrary files.
**Learning:** This existed because `ZipInputStream` entries contain names that can include relative paths (e.g., `../`), and directly appending them to the target directory creates a file object pointing outside the target.
**Prevention:** Always validate that the canonical path of the extracted file starts with the canonical path of the target directory when extracting archives.
