# Launcher Cannot Be Updated

If you are trying to update FocusLauncher but the installation fails, you are most likely trying
to install a build that was signed with a different signing key than the one currently installed.
Android refuses to update an app in place when the new APK's signature does not match the
installed one.

## How FocusLauncher is distributed

FocusLauncher is preparing its first signed APK for
[GitHub Releases](https://github.com/siudajakub/focuslauncher/releases), but no public release
exists as of 2026-07-16. After release, always update by installing an APK with the same application
ID and signing identity as the installed build.

Release, debug, and nightly builds use different application IDs, so they install side by side
rather than updating one another. Release and nightly CI builds use the same owner-held signing
identity; local debug builds use the Android debug key.

## If the update still fails

If you previously installed a build signed with a different key (for example a self-built debug
APK), you cannot update it in place with a future official release. To switch:

1. Back up your data in Settings > Backup & restore.
2. Uninstall the existing version.
3. Install the new APK.
4. Restore your data in Settings > Backup & restore > Restore.

::: warning
The backup file format isn't guaranteed to be backward compatible. To ensure that all data is
restored correctly, restore a backup into the same or a newer version than the one that created it.
:::
