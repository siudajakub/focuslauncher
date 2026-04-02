package de.mm20.launcher2.ui.settings.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.ListPreference
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focusreport.FocusReportSettingsRoute
import de.mm20.launcher2.ui.settings.focusapps.FocusAppsSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchSettingsRoute : NavKey

private fun focusTimeOptions(): List<Pair<String, Int>> {
    return buildList {
        for (minutes in 0 until 24 * 60 step 30) {
            add(formatFocusTime(minutes) to minutes)
        }
    }
}

private fun formatFocusTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}

@Composable
fun SearchSettingsScreen() {

    val viewModel: SearchSettingsScreenVM = viewModel()

    val backStack = LocalBackStack.current

    val favorites by viewModel.favorites.collectAsStateWithLifecycle(null)
    val allApps by viewModel.allApps.collectAsStateWithLifecycle(null)

    val autoFocus by viewModel.autoFocus.collectAsStateWithLifecycle(null)
    val launchOnEnter by viewModel.launchOnEnter.collectAsStateWithLifecycle(null)
    val reverseSearchResults by viewModel.reverseSearchResults.collectAsStateWithLifecycle(null)
    val focusModeEnabled by viewModel.focusModeEnabled.collectAsStateWithLifecycle(null)
    val focusHideDistractingApps by viewModel.focusHideDistractingApps.collectAsStateWithLifecycle(null)
    val focusDefaultDelaySeconds by viewModel.focusDefaultDelaySeconds.collectAsStateWithLifecycle(null)
    val focusDefaultSessionMinutes by viewModel.focusDefaultSessionMinutes.collectAsStateWithLifecycle(null)
    val focusEnableDnd by viewModel.focusEnableDnd.collectAsStateWithLifecycle(null)
    val focusProductivityTimeEnabled by viewModel.focusProductivityTimeEnabled.collectAsStateWithLifecycle(null)
    val focusProductivityWindow1StartMinutes by viewModel.focusProductivityWindow1StartMinutes.collectAsStateWithLifecycle(null)
    val focusProductivityWindow1EndMinutes by viewModel.focusProductivityWindow1EndMinutes.collectAsStateWithLifecycle(null)
    val focusProductivityWindow2StartMinutes by viewModel.focusProductivityWindow2StartMinutes.collectAsStateWithLifecycle(null)
    val focusProductivityWindow2EndMinutes by viewModel.focusProductivityWindow2EndMinutes.collectAsStateWithLifecycle(null)
    val productivityTimeOptions = focusTimeOptions()

    PreferenceScreen(title = stringResource(R.string.focus_settings_section_title)) {
        item {
            PreferenceCategory(
                title = stringResource(R.string.focus_settings_section_title)
            ) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.apps_24px,
                    text = stringResource(R.string.search_apps_only_message),
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_mode_enabled),
                    summary = stringResource(R.string.focus_settings_mode_enabled_summary),
                    icon = R.drawable.timer_24px,
                    value = focusModeEnabled == true,
                    onValueChanged = {
                        viewModel.setFocusModeEnabled(it)
                    },
                )
                AnimatedVisibility(focusModeEnabled == true) {
                    Column {
                        SwitchPreference(
                            title = stringResource(R.string.focus_settings_hide_distracting),
                            summary = stringResource(R.string.focus_settings_hide_distracting_summary),
                            icon = R.drawable.visibility_off_24px,
                            value = focusHideDistractingApps == true,
                            onValueChanged = {
                                viewModel.setFocusHideDistractingApps(it)
                            },
                        )
                        Preference(
                            title = stringResource(R.string.focus_apps_title),
                            summary = stringResource(R.string.focus_apps_summary),
                            icon = R.drawable.apps_24px,
                            onClick = {
                                backStack.add(FocusAppsSettingsRoute)
                            }
                        )
                        ListPreference(
                            title = stringResource(R.string.focus_settings_delay_global_title),
                            items = (0..15).map { "${it}s" to it },
                            value = focusDefaultDelaySeconds,
                            onValueChanged = {
                                if (it != null) viewModel.setFocusDefaultDelaySeconds(it)
                            },
                            icon = R.drawable.timer_24px,
                        )
                        ListPreference(
                            title = stringResource(R.string.focus_settings_session_global_title),
                            items = listOf(5, 10, 15, 20, 25, 30, 45, 60).map { "${it} min" to it },
                            value = focusDefaultSessionMinutes,
                            onValueChanged = {
                                if (it != null) viewModel.setFocusDefaultSessionMinutes(it)
                            },
                            icon = R.drawable.schedule_24px,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.focus_settings_enable_dnd),
                            summary = stringResource(R.string.focus_settings_enable_dnd_summary),
                            icon = R.drawable.notifications_24px,
                            value = focusEnableDnd == true,
                            onValueChanged = {
                                viewModel.setFocusEnableDnd(it)
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.focus_settings_productivity_time),
                            summary = stringResource(R.string.focus_settings_productivity_time_summary),
                            icon = R.drawable.timer_24px,
                            value = focusProductivityTimeEnabled == true,
                            onValueChanged = {
                                viewModel.setFocusProductivityTimeEnabled(it)
                            },
                        )
                        AnimatedVisibility(focusProductivityTimeEnabled == true) {
                            Column {
                                ListPreference(
                                    title = stringResource(R.string.focus_settings_productivity_window_1_start),
                                    items = productivityTimeOptions,
                                    value = focusProductivityWindow1StartMinutes,
                                    onValueChanged = {
                                        if (it != null) viewModel.setFocusProductivityWindow1StartMinutes(it)
                                    },
                                    icon = R.drawable.schedule_24px,
                                )
                                ListPreference(
                                    title = stringResource(R.string.focus_settings_productivity_window_1_end),
                                    items = productivityTimeOptions,
                                    value = focusProductivityWindow1EndMinutes,
                                    onValueChanged = {
                                        if (it != null) viewModel.setFocusProductivityWindow1EndMinutes(it)
                                    },
                                    icon = R.drawable.schedule_24px,
                                )
                                ListPreference(
                                    title = stringResource(R.string.focus_settings_productivity_window_2_start),
                                    items = productivityTimeOptions,
                                    value = focusProductivityWindow2StartMinutes,
                                    onValueChanged = {
                                        if (it != null) viewModel.setFocusProductivityWindow2StartMinutes(it)
                                    },
                                    icon = R.drawable.schedule_24px,
                                )
                                ListPreference(
                                    title = stringResource(R.string.focus_settings_productivity_window_2_end),
                                    items = productivityTimeOptions,
                                    value = focusProductivityWindow2EndMinutes,
                                    onValueChanged = {
                                        if (it != null) viewModel.setFocusProductivityWindow2EndMinutes(it)
                                    },
                                    icon = R.drawable.schedule_24px,
                                )
                            }
                        }
                        Preference(
                            title = stringResource(R.string.focus_report_title),
                            summary = stringResource(R.string.focus_report_summary),
                            icon = R.drawable.query_stats_24px,
                            onClick = {
                                backStack.add(FocusReportSettingsRoute)
                            }
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.preference_screen_search)) {
                SwitchPreference(
                    title = stringResource(R.string.preference_search_favorites),
                    summary = stringResource(R.string.preference_search_favorites_summary),
                    icon = R.drawable.star_24px,
                    value = favorites == true,
                    onValueChanged = {
                        viewModel.setFavorites(it)
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.preference_search_apps),
                    summary = stringResource(R.string.preference_search_apps_summary),
                    icon = R.drawable.apps_24px,
                    value = allApps == true,
                    onValueChanged = {
                        viewModel.setAllApps(it)
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.preference_search_bar_auto_focus),
                    summary = stringResource(R.string.preference_search_bar_auto_focus_summary),
                    icon = R.drawable.keyboard_24px,
                    value = autoFocus == true,
                    onValueChanged = {
                        viewModel.setAutoFocus(it)
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.preference_search_bar_launch_on_enter),
                    iconPadding = true,
                    summary = stringResource(R.string.preference_search_bar_launch_on_enter_summary),
                    value = launchOnEnter == true,
                    onValueChanged = {
                        viewModel.setLaunchOnEnter(it)
                    }
                )
                ListPreference(
                    title = stringResource(R.string.preference_layout_search_results),
                    items = listOf(
                        stringResource(R.string.search_results_order_top_down) to false,
                        stringResource(R.string.search_results_order_bottom_up) to true,
                    ),
                    value = reverseSearchResults,
                    onValueChanged = {
                        if (it != null) viewModel.setReverseSearchResults(it)
                    },
                    icon = R.drawable.sort_24px
                )
            }
        }
    }

}
