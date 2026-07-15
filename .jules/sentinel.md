## 2024-05-24 - Zip Slip Vulnerability in BackupManager

**Vulnerability:** A Zip Slip (Path Traversal) vulnerability existed in `BackupManager.kt` during archive extraction, allowing malicious zip files to escape the destination directory by providing relative paths like `../malicious.sh`.
**Learning:** The vulnerability existed because the `extractArchive` function blindly trusted the `ZipEntry` name and directly appended it to the target output directory without validating if the resolved path actually fell within the target directory boundary.
**Prevention:** Always sanitize and validate `ZipEntry` names during extraction. Specifically, verify that the `canonicalPath` of the created `File` starts with the `canonicalPath` (plus `File.separator`) of the destination directory.
