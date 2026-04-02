package de.mm20.launcher2.ui.settings.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.advanced.AdvancedSettingsRoute
import de.mm20.launcher2.ui.settings.appearance.AppearanceSettingsRoute
import de.mm20.launcher2.ui.settings.homescreen.HomescreenSettingsRoute
import de.mm20.launcher2.ui.settings.locale.LocaleSettingsRoute
import de.mm20.launcher2.ui.settings.search.SearchSettingsRoute
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
                    icon = R.drawable.palette_24px,
                    title = stringResource(id = R.string.preference_screen_appearance),
                    summary = stringResource(id = R.string.preference_screen_appearance_summary),
                    onClick = {
                        backStack.add(AppearanceSettingsRoute)
                    }
                )
                Preference(
                    icon = R.drawable.home_24px,
                    title = stringResource(id = R.string.preference_screen_homescreen),
                    summary = stringResource(id = R.string.preference_screen_homescreen_summary),
                    onClick = {
                        backStack.add(HomescreenSettingsRoute)
                    }
                )
                Preference(
                    icon = R.drawable.search_24px,
                    title = stringResource(id = R.string.focus_settings_section_title),
                    summary = stringResource(id = R.string.focus_settings_entry_summary),
                    onClick = {
                        backStack.add(SearchSettingsRoute)
                    }
                )
                Preference(
                    icon = R.drawable.translate_24px,
                    title = stringResource(id = R.string.preference_screen_locale),
                    summary = stringResource(id = R.string.preference_screen_locale_summary),
                    onClick = {
                        backStack.add(LocaleSettingsRoute)
                    }
                )
                Preference(
                    icon = R.drawable.more_vert_24px,
                    title = stringResource(id = R.string.preference_screen_advanced),
                    summary = stringResource(id = R.string.preference_screen_advanced_summary),
                    onClick = {
                        backStack.add(AdvancedSettingsRoute)
                    }
                )
            }
        }
    }
}
