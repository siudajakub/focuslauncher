# Crash Reporter

When the launcher crashes, a notification is posted. When you tap it, the crash reporter opens. You
can also navigate to Launcher settings > Advanced > Debug > Crash reporter.

The crash reporter lists crashes and exceptions.

::: warning
The in-app report button currently targets the upstream Kvaesitso repository. Until
[issue #104](https://github.com/siudajakub/focuslauncher/issues/104) is fixed, open reports directly
in the [FocusLauncher issue tracker](https://github.com/siudajakub/focuslauncher/issues/new).
:::

## Crashes

Crashes are marked with the <span class="material-symbols-rounded">error</span> icon. Crashes are
unexpected errors that were not handled by the launcher and should be reported. Copy the report and
create a new FocusLauncher issue using the link above. Include steps to reproduce and what you were
doing when the crash occurred.

[Read more about reporting bugs](/docs/contributor-guide/report-bugs).

## Exceptions

Exceptions are marked with the <span class="material-symbols-rounded">warning</span> icon. Exceptions are errors that were handled by the launcher. They can sometimes be helpful to locate bugs and other sources of errors, but as long as you don't notice anything strange, you can safely ignore them and do not need to report them. It is expected that some exceptions will occur while the launcher is running. For example, the most common source of exceptions is network timeouts due to the device being offline.
