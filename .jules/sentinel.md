
## 2026-07-09 - Prevent Zip Slip Vulnerability in Archive Extraction
**Vulnerability:** Path traversal (Zip Slip) vulnerability found in `BackupManager.extractArchive` where zip entries could potentially overwrite files outside the intended destination directory.
**Learning:** `ZipInputStream.nextEntry` does not inherently protect against relative path traversal characters (e.g., `../`). Partial path matching is also a risk if canonical paths are checked with `.startsWith()` without appending `File.separator`.
**Prevention:** Always validate extracted files by checking if their `canonicalPath` strictly starts with the destination directory's `canonicalPath` plus `File.separator` to avoid partial-path string matching bugs (e.g. `/data/app` vs `/data/app-suffix`).
