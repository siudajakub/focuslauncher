package de.mm20.launcher2.ui.settings.advanced

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.about.AboutSettingsRoute
import de.mm20.launcher2.ui.settings.backup.BackupSettingsRoute
import de.mm20.launcher2.ui.settings.debug.DebugSettingsRoute
import de.mm20.launcher2.ui.settings.gestures.GesturesSettingsRoute
import de.mm20.launcher2.ui.settings.icons.IconsSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object AdvancedSettingsRoute : NavKey

@Composable
fun AdvancedSettingsScreen() {
    val backStack = LocalBackStack.current

    PreferenceScreen(title = stringResource(R.string.preference_screen_advanced)) {
        item {
            PreferenceCategory(title = stringResource(R.string.preference_advanced_optional_title)) {
                Preference(
                    icon = R.drawable.apps_24px,
                    title = stringResource(R.string.preference_screen_icons),
                    summary = stringResource(R.string.preference_screen_icons_summary),
                    onClick = { backStack.add(IconsSettingsRoute) }
                )
                Preference(
                    icon = R.drawable.gesture_24px,
                    title = stringResource(R.string.preference_screen_gestures),
                    summary = stringResource(R.string.preference_screen_gestures_summary),
                    onClick = { backStack.add(GesturesSettingsRoute) }
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.preference_advanced_maintenance_title)) {
                Preference(
                    icon = R.drawable.settings_backup_restore_24px,
                    title = stringResource(R.string.preference_screen_backup),
                    summary = stringResource(R.string.preference_screen_backup_summary),
                    onClick = { backStack.add(BackupSettingsRoute) }
                )
                Preference(
                    icon = R.drawable.bug_report_24px,
                    title = stringResource(R.string.preference_screen_debug),
                    summary = stringResource(R.string.preference_screen_debug_summary),
                    onClick = { backStack.add(DebugSettingsRoute) }
                )
                Preference(
                    icon = R.drawable.info_24px,
                    title = stringResource(R.string.preference_screen_about),
                    summary = stringResource(R.string.preference_screen_about_summary),
                    onClick = { backStack.add(AboutSettingsRoute) }
                )
            }
        }
    }
}
