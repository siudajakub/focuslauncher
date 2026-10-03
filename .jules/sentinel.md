## 2024-05-15 - Path Traversal (Zip Slip) in Backup Manager
**Vulnerability:** The `extractArchive` function in `BackupManager` extracted zip files without validating if the destination path remained within the intended directory, allowing for potential Zip Slip attacks via crafted Zip entries containing `../`.
**Learning:** When extracting zip archives, always compare the canonical path of the resolved file with the canonical path of the intended output directory (including a file separator) to prevent writing files outside the intended scope.
**Prevention:** Use `File.canonicalPath` and verify it starts with `canonicalOutDir + File.separator` before writing extracted data.
