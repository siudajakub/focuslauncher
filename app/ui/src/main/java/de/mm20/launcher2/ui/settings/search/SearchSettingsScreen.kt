package de.mm20.launcher2.ui.settings.search

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.mm20.launcher2.ui.launcher.focus.FocusInsightsRoute
import de.mm20.launcher2.ui.settings.focusapps.FocusAppsSettingsRoute
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchSettingsRoute : NavKey

private fun formatFocusTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}

private enum class ProductivityWindowField {
    Start,
    End,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductivityTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.focus_settings_productivity_window_dialog_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(state.hour * 60 + state.minute)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
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
    val focusProductivityWindows by viewModel.focusProductivityWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    var editingWindowIndex by remember { mutableIntStateOf(-1) }
    var editingWindowField by remember { mutableStateOf<ProductivityWindowField?>(null) }

    PreferenceScreen(title = stringResource(R.string.focus_settings_section_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_section_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_mode_enabled),
                    summary = stringResource(R.string.focus_settings_mode_enabled_summary),
                    icon = R.drawable.timer_24px,
                    value = focusModeEnabled == true,
                    onValueChanged = {
                        viewModel.setFocusModeEnabled(it)
                    },
                )
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.apps_24px,
                    text = stringResource(R.string.search_apps_only_message),
                )
                Preference(
                    title = stringResource(R.string.focus_apps_title),
                    summary = stringResource(R.string.focus_apps_summary),
                    icon = R.drawable.apps_24px,
                    onClick = {
                        backStack.add(FocusAppsSettingsRoute)
                    }
                )
                Preference(
                    title = stringResource(R.string.focus_support_title),
                    summary = stringResource(R.string.focus_support_summary),
                    icon = R.drawable.emoji_objects_24px,
                    onClick = {
                        backStack.add(FocusSupportSettingsRoute)
                    }
                )
                Preference(
                    title = stringResource(R.string.focus_insights_title),
                    summary = stringResource(R.string.focus_settings_reports_summary),
                    icon = R.drawable.query_stats_24px,
                    onClick = {
                        backStack.add(FocusInsightsRoute)
                    }
                )
            }
        }
        item {
            AnimatedVisibility(focusModeEnabled == true) {
                PreferenceCategory(title = stringResource(R.string.preference_category_advanced)) {
                    SwitchPreference(
                        title = stringResource(R.string.focus_settings_hide_distracting),
                        summary = stringResource(R.string.focus_settings_hide_distracting_summary),
                        icon = R.drawable.visibility_off_24px,
                        value = focusHideDistractingApps == true,
                        onValueChanged = {
                            viewModel.setFocusHideDistractingApps(it)
                        },
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
                }
            }
        }
        item {
            AnimatedVisibility(focusModeEnabled == true && focusProductivityTimeEnabled == true) {
                PreferenceCategory(title = stringResource(R.string.focus_settings_productivity_windows_title)) {
                    focusProductivityWindows.forEachIndexed { index, window ->
                        Preference(
                            title = stringResource(
                                R.string.focus_settings_productivity_window_title,
                                index + 1,
                            ),
                            summary = "${formatFocusTime(window.startMinutes)} - ${formatFocusTime(window.endMinutes)}",
                            icon = R.drawable.schedule_24px,
                            onClick = {
                                editingWindowIndex = index
                                editingWindowField = ProductivityWindowField.Start
                            },
                        )
                        Preference(
                            title = stringResource(R.string.focus_settings_productivity_window_start),
                            summary = formatFocusTime(window.startMinutes),
                            icon = R.drawable.schedule_24px,
                            onClick = {
                                editingWindowIndex = index
                                editingWindowField = ProductivityWindowField.Start
                            },
                        )
                        Preference(
                            title = stringResource(R.string.focus_settings_productivity_window_end),
                            summary = formatFocusTime(window.endMinutes),
                            icon = R.drawable.schedule_24px,
                            onClick = {
                                editingWindowIndex = index
                                editingWindowField = ProductivityWindowField.End
                            },
                        )
                        if (focusProductivityWindows.size > 1) {
                            Preference(
                                title = stringResource(R.string.focus_settings_productivity_remove_window),
                                icon = R.drawable.delete_24px,
                                onClick = { viewModel.removeFocusProductivityWindow(index) },
                            )
                        }
                    }

                    if (focusProductivityWindows.size < 3) {
                        Preference(
                            title = stringResource(R.string.focus_settings_productivity_add_window),
                            summary = stringResource(R.string.focus_settings_productivity_add_window_summary),
                            icon = R.drawable.add_24px,
                            onClick = { viewModel.addFocusProductivityWindow() },
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

    val activeWindow = focusProductivityWindows.getOrNull(editingWindowIndex)
    val initialMinutes = when (editingWindowField) {
        ProductivityWindowField.Start -> activeWindow?.startMinutes
        ProductivityWindowField.End -> activeWindow?.endMinutes
        null -> null
    }
    if (initialMinutes != null && editingWindowField != null && editingWindowIndex >= 0) {
        ProductivityTimePickerDialog(
            title = when (editingWindowField) {
                ProductivityWindowField.Start -> stringResource(R.string.focus_settings_productivity_window_start)
                ProductivityWindowField.End -> stringResource(R.string.focus_settings_productivity_window_end)
                null -> ""
            },
            initialMinutes = initialMinutes,
            onDismiss = {
                editingWindowIndex = -1
                editingWindowField = null
            },
            onTimeSelected = { minutes ->
                when (editingWindowField) {
                    ProductivityWindowField.Start -> viewModel.updateFocusProductivityWindowStart(editingWindowIndex, minutes)
                    ProductivityWindowField.End -> viewModel.updateFocusProductivityWindowEnd(editingWindowIndex, minutes)
                    null -> Unit
                }
                editingWindowIndex = -1
                editingWindowField = null
            },
        )
    }

}
