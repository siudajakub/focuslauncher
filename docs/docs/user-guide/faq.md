---
sidebar_position: 2
---

# Frequently Asked Questions

## How do I get app icons on the home screen?

There are two options:

1. Enable Settings > Home screen > Dock.

2. Scroll down to the end of the widgets list, select "Edit widgets" > "Add widget" > "Favorites" to
   add
   the favorites widget to the home screen.

## What are these categories "Personal", "Private", and "Work" above the app grid?

These categories aren't a feature of the launcher, but just a representation of Android's different
profiles. Profiles are isolated spaces on your device that allow you to have separate apps, accounts,
and data, independent of the main profile.

**Personal** is the main profile.

**Work** is a work profile. Learn more about work
profiles [here](https://www.android.com/enterprise/work-profile/).
You can also use an app
like [Insular](https://f-droid.org/de/packages/com.oasisfeng.island.fdroid/)
or [Shelter](https://f-droid.org/de/packages/net.typeblog.shelter/) to create and manage a work
profile.

**Private** is the private space profile, available on Android 15 and higher. Learn more about
private spaces [here](https://support.google.com/android/answer/15341885?hl=en).

## Can I remove / customize the clock?

Yes, you can customize the clock style by going to Settings > Home screen > Clock and selecting a
different
style. There is also an "empty style" that will remove the clock entirely.

## The toggle to grant notification access or to enable the accessibility service is disabled

Please refer to
the [Restricted Settings on Android 13+](/docs/user-guide/troubleshooting/restricted-settings) page.

## The launcher keeps asking for notification access or accessibility service

Please refer to [this page](/docs/user-guide/troubleshooting/granted-permissions).

## I can't update to the latest version

Please refer to
the [Launcher Cannot Be Updated](/docs/user-guide/troubleshooting/update-not-installed) page.

## Why is wallpaper blur not supported on my device?

Wallpaper blur is available if:

- the device runs Android 12 or higher
- battery saver is not enabled

Furthermore, the device has to have support for [cross window
blur](https://source.android.com/docs/core/display/window-blurs). This is a flag that has to
be enabled by the device manufacturer to indicate that their implementation of the render engine
supports blur effects and that the GPU is powerful enough to handle them.

## Where do I download FocusLauncher?

FocusLauncher is distributed as a signed APK on
[GitHub Releases](https://github.com/siudajakub/focuslauncher/releases). Download the latest
release and install it on your device. See the [Get Started](/docs/user-guide/) page for
details.

## Will FocusLauncher ever be available on the Play Store?

No. FocusLauncher is distributed only through GitHub Releases, not the Google Play Store or
F-Droid.