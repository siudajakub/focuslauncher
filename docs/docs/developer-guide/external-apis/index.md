# External APIs

FocusLauncher integrates with a small number of external APIs, inherited from upstream Kvaesitso. Most of them require some sort of authentication, like an API key. These API keys are not part of the GitHub repository. If you want to build FocusLauncher from source with all optional features enabled, follow the steps in this chapter.

> [!INFO]
> FocusLauncher is still buildable even without these steps, but some optional features (such as weather) will be disabled in the resulting APK. If all you need is a debug build for testing purposes, you can probably skip this chapter.

> [!NOTE]
> FocusLauncher removed several upstream online integrations, including web/Wikipedia search and cloud accounts. The remaining external APIs are limited to opt-in features like weather and currency exchange rates.
