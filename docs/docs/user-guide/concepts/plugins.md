# Plugins

Plugins are a way to extend the launcher's functionality. Plugins act as data providers for
launcher components.

> [!NOTE]
> The plugin system is inherited from upstream Kvaesitso and is an advanced, developer-oriented
> surface in FocusLauncher. FocusLauncher removed several upstream search subsystems (file,
> contact, and location/places search), so plugins that provide those result types no longer have
> anything to plug into. The weather provider plugin contract still works as an opt-in feature.

## Usage

### Installation

Plugins are installed the same way as any other app. You can either download them from an app store,
or install them manually as APK files.

### Activation

After the plugin has been installed, it needs to be activated in the launcher settings. To do this,
go to settings > plugins, tap on the plugin you want to activate, and enable it. The plugin will
show a permission dialog. Tap on "Allow" to give the launcher the necessary permission to access
the plugin's data.

Next, you may or may not need to perform steps to configure the plugin. For example, a plugin might
require you to sign in with an account in order to use it. A banner is shown when additional
configuration steps are needed:

<img src="/img/plugin-configuration.png" width="300"/>

Last but not least, you need to enable the plugin functionalities that you want to use.

For weather plugins, you need to change the weather integration settings to use the plugin as
weather provider.

For search plugins, you need to enable the search provider in search settings. Shortcuts to these
settings are available on the plugin's settings page.

## Available plugins

Weather provider plugins remain usable as an opt-in feature. The following weather plugins were
published for upstream Kvaesitso and use the same SDK:

- **OpenWeatherMap plugin**: adds the OpenWeatherMap weather
  provider [[GitHub]](https://github.com/Kvaesitso/Plugin-OpenWeatherMap)
- **Breezy Weather plugin**: forwards weather data from Breezy
  Weather [[GitHub]](https://github.com/Kvaesitso/Plugin-BreezyWeather)
- **Meteo.lt**: weather data from [meteo.lt](https://meteo.lt), for
  Lithuania [[GitHub]](https://github.com/leekleak/KvaesitsoMeteoLT) [[Download]](https://github.com/leekleak/KvaesitsoMeteoLT/releases)

> [!WARNING]
> Upstream also publishes file-search and places/transport plugins (OneDrive, HERE, Foursquare,
> public transport) and a Tasks.org plugin. FocusLauncher removed the file, contact, and
> location/places search subsystems, so those plugins are not useful here. They are intentionally
> not listed.
>
> TODO: confirm which of these weather plugins are verified against the current FocusLauncher
> build before promoting them as officially supported.

## Plugin development

If you are a developer and you are interested in developing your own plugin for the launcher,
you can find more information in the [developer guide](/docs/developer-guide/plugins/get-started).
