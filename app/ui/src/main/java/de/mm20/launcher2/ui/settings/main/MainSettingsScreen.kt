package de.mm20.launcher2.ui.settings.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focussettings.FocusSettingsRoute
import de.mm20.launcher2.ui.settings.launchersettings.LauncherSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute: NavKey

@Composable
fun MainSettingsScreen() {
    val backStack = LocalBackStack.current
    PreferenceScreen(
        title = stringResource(R.string.settings),
    ) {
        item {
            PreferenceCategory {
                Preference(
                    icon = R.drawable.timer_24px,
                    title = stringResource(id = R.string.focus_settings_title),
                    summary = stringResource(id = R.string.focus_settings_summary),
                    onClick = {
                        backStack.add(FocusSettingsRoute)
                    }
                )
                Preference(
                    icon = R.drawable.settings_24px,
                    title = stringResource(id = R.string.launcher_settings_title),
                    summary = stringResource(id = R.string.launcher_settings_summary),
                    onClick = {
                        backStack.add(LauncherSettingsRoute)
                    }
                )
            }
        }
    }
}
