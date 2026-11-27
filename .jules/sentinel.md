## 2024-07-11 - [Path Traversal in BackupManager]
**Vulnerability:** Path traversal (Zip Slip) vulnerability in `extractArchive` method of `BackupManager` where unvalidated zip entry names are used to create files during extraction.
**Learning:** `ZipInputStream.nextEntry` does not sanitize zip entry names, and malicious zip files can contain relative paths (e.g. `../`) to extract files outside the intended destination directory.
**Prevention:** Always validate that the canonical path of the file to be extracted starts with the canonical path of the intended destination directory, appending `File.separator` to avoid partial directory name matching.
